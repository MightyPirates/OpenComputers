local pipes = require("pipes")
local event = require("event")
local process = require("process")
local computer = require("computer")

local thread = {}

do
  local handlers = event.handlers
  local handlers_mt = getmetatable(handlers)
  -- the event library sets a metatable on handlers, but we set threaded=true
  if not handlers_mt.threaded then
    -- find the root process
    local root_data
    for _,p in pairs(process.list) do
      if not p.parent then
        root_data = p.data
        break
      end
    end
    assert(root_data, "thread library panic: no root proc")
    handlers_mt.threaded = true
    -- if we don't separate root handlers from thread handlers we see double dispatch
    -- because the thread calls dispatch on pull as well
    root_data.handlers = {} -- root handlers
    root_data.pull = handlers_mt.__call -- the real computer.pullSignal
    while true do
      local key, value = next(handlers)
      if not key then break end
      root_data.handlers[key] = value
      handlers[key] = nil
    end
    handlers_mt.__index = function(_, key)
      return process.info().data.handlers[key]
    end
    handlers_mt.__newindex = function(_, key, value)
      process.info().data.handlers[key] = value
    end
    handlers_mt.__pairs = function(_, ...)
      return pairs(process.info().data.handlers, ...)
    end
    handlers_mt.__call = function(tbl, ...)
      return process.info().data.pull(tbl, ...)
    end
  end
end

local function waitForDeath(threads, timeout, all)
  checkArg(1, threads, "table")
  checkArg(2, timeout, "number", "nil")
  checkArg(3, all, "boolean")
  timeout = timeout or math.huge
  local mortician = {}
  local timed_out = true
  local deadline = computer.uptime() + timeout
  while deadline > computer.uptime() do
    local dieing = {}
    local living = false
    for _,t in ipairs(threads) do
      local result = t.process and t.process.data.result
      local proc_ok = type(result) ~= "table" or result[1]
      local ready_to_die = t:status() ~= "running" -- suspended is considered dead to exit
        or not proc_ok -- the thread is killed if its attached process has a non zero exit
      if ready_to_die then
        dieing[#dieing + 1] = t
        mortician[t] = true
      else
        living = true
      end
    end

    if all and not living or not all and #dieing > 0 then
      timed_out = false
      break
    end

    -- resume each non dead thread
    -- we KNOW all threads are event.pull blocked
    event.pull(deadline - computer.uptime())
  end

  for t in pairs(mortician) do
    t:kill()
  end

  if timed_out then
    return nil, "thread join timed out"
  end
  return true
end

function thread.waitForAny(threads, timeout)
  return waitForDeath(threads, timeout, false)
end

function thread.waitForAll(threads, timeout)
  return waitForDeath(threads, timeout, true)
end

local box_thread = {}
local box_thread_handle = {close = thread.waitForAll}

local function get_box_thread_handle(handles, bCreate)
  for _,next_handle in ipairs(handles) do
    local btm_mt = getmetatable(next_handle)
    if btm_mt and btm_mt.__index == box_thread_handle then
      return next_handle
    end
  end
  if bCreate then
    local btm = setmetatable({}, {__index = box_thread_handle})
    table.insert(handles, btm)
    return btm
  end
end

function box_thread:resume()
  if self:status() ~= "suspended" then
    return nil, "cannot resume " .. self:status() .. " thread"
  end
  getmetatable(self).__status = "running"
end

function box_thread:suspend()
  if self:status() ~= "running" then
    return nil, "cannot suspend " .. self:status() .. " thread"
  end
  getmetatable(self).__status = "suspended"
end

function box_thread:status()
  return getmetatable(self).__status
end

function box_thread:join(timeout)
  return box_thread_handle.close({self}, timeout)
end

function box_thread:kill()
  self:detach()
  if self:status() == "dead" then
    return
  end
  getmetatable(self).__status = "dead"
  self.pco.stack = {}
end

function box_thread:detach()
  if not self.process then
    return
  end
  local handles = self.process.data.handles
  local btHandle = get_box_thread_handle(handles)
  if not btHandle then
    return nil, "thread failed to detach, process had no thread handle"
  end
  for i,h in ipairs(btHandle) do
    if h == self then
      self.process = nil
      return table.remove(btHandle, i)
    end
  end
  return nil, "thread not found in parent process"
end

function box_thread:attach(parent)
  checkArg(1, parent, "thread", "number", "nil")
  local proc = process.info(parent)
  if not proc then return nil, "thread failed to attach, process not found" end
  self:detach()
  -- attach to parent or the current process
  self.process = proc
  local handles = self.process.data.handles

  -- this process may not have a box_thread manager handle
  local btHandle = get_box_thread_handle(handles, true)
  table.insert(btHandle, self)
  return true
end

function thread.create(fp, ...)
  checkArg(1, fp, "function")

  local t = setmetatable({}, {__status="suspended",__index=box_thread})
  t.pco = pipes.createCoroutineStack(function(...)
    getmetatable(t).__status = "running"
    local fp_co = t.pco.create(fp)
    -- run fp_co until dead
    -- pullSignal will yield_all past this point
    -- but yield will return here, we pullSignal from here to yield_all
    local args = table.pack(...)
    while true do
      local result = table.pack(t.pco.resume(fp_co, table.unpack(args, 1, args.n)))
      if t.pco.status(fp_co) == "dead" then
        -- this error handling is VERY much like process.lua
        -- maybe one day it'll merge
        if not result[1] then
          local exit_code
          local msg = result[2]
          -- msg can be a custom error object
          local reason = "crashed"
          if type(msg) == "table" then
            if type(msg.reason) == "string" then
              reason = msg.reason
            end
            exit_code = tonumber(msg.code)
          elseif type(msg) == "string" then
            reason = msg
          end
          if not exit_code then
            pcall(event.onError, string.format("[thread] %s", reason))
            exit_code = 1
          end
          os.exit(exit_code)
        end
        break
      end
      args = table.pack(event.pull(table.unpack(result, 2, result.n)))
    end
  end)

  --special resume to keep track of process death
  local function private_resume(...)
    local result = table.pack(t.pco.resume_all(...))
    if #t.pco.stack == 0 then
      t:detach()
      local mt = getmetatable(t)
      mt.__status = "dead"
      event.push("thread_exit")
    end
    return table.unpack(result, 1, result.n)
  end

  local data = process.info(t.pco.stack[1]).data
  data.handlers = {}
  data.pull = function(_, timeout)
    -- register a timeout handler
    -- hack so that event.register sees the root handlers
    local data_handlers = data.handlers
    data.handlers = process.info(2).data.handlers
    event.register(
      nil, -- nil key matches anything, timers use false keys
      private_resume,
      timeout, -- wait for the time specified by the caller
      1) -- we only want this thread to wake up once
    data.handlers = data_handlers

    -- yield_all will yield this pco stack
    -- the callback will resume this stack
    local event_data
    repeat
      event_data = table.pack(t.pco.yield_all(timeout))
      -- during sleep, we may have been suspended
    until t:status() ~= "suspended"
    return table.unpack(event_data, 1, event_data.n)
  end

  t:attach()
  private_resume(...) -- threads start out running

  return t
end

return thread
