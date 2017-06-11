local pipes = require("pipes")
local event = require("event")
local process = require("process")
local computer = require("computer")

local thread = {}

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
    local living = {}
    for _,t in ipairs(threads) do
      local result = t.process and t.process.data.result
      local proc_ok = type(result) ~= "table" or result[1]
      local ready_to_die = t:status() ~= "running" -- suspended is considered dead to exit
        or not proc_ok -- the thread is killed if its attached process has a non zero exit
      if ready_to_die then
        dieing[#dieing + 1] = t
        mortician[t] = true
      else
        living[#living + 1] = t
      end
    end

    if all and #living == 0 or not all and #dieing > 0 then
      timed_out = false
      break
    end

    -- resume each non dead thread
    -- we KNOW all threads are event.pull blocked
    event.pull()
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
local box_thread_handle = {}
box_thread_handle.close = thread.waitForAll

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
  self:detach()
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
    local mt = getmetatable(t)
    mt.__status = "running"
    local fp_co = t.pco.create(fp)
    -- run fp_co until dead
    -- pullSignal will yield_all past this point
    -- but yield will return here, we pullSignal from here to yield_all
    local args = table.pack(...)
    while true do
      local result = table.pack(t.pco.resume(fp_co, table.unpack(args, 1, args.n)))
      if t.pco.status(fp_co) == "dead" then
        if not result[1] then
          event.onError(string.format("thread crashed: %s", tostring(result[2])))
        end
        break
      end
      args = table.pack(event.pull(table.unpack(result, 2, result.n)))
    end
    mt.__status = "dead"
    event.push("thread_exit")
    t:detach()
  end)
  local handlers = event.handlers
  local handlers_mt = getmetatable(handlers)
  -- the event library sets a metatable on handlers
  -- but not a pull field
  if not handlers_mt.pull then
    -- if we don't separate root handlers from thread handlers we see double dispatch
    -- because the thread calls dispatch on pull as well
    handlers_mt.handlers = {} -- root handlers
    handlers_mt.pull = handlers_mt.__call -- the real computer.pullSignal
    handlers_mt.current = function(field) return process.info().data[field] or handlers_mt[field] end
    while true do
      local key, value = next(handlers)
      if not key then break end
      handlers_mt.handlers[key] = value
      handlers[key] = nil
    end
    handlers_mt.__index = function(_, key)
      return handlers_mt.current("handlers")[key]
    end
    handlers_mt.__newindex = function(_, key, value)
      handlers_mt.current("handlers")[key] = value
    end
    handlers_mt.__pairs = function(_, ...)
      return pairs(handlers_mt.current("handlers"), ...)
    end
    handlers_mt.__call = function(tbl, ...)
      return handlers_mt.current("pull")(tbl, ...)
    end
  end

  local data = process.info(t.pco.stack[1]).data
  data.handlers = {}
  data.pull = function(_, timeout)
    -- register a timeout handler
    -- hack so that event.register sees the root handlers
    local data_handlers = data.handlers
    data.handlers = handlers_mt.handlers
    event.register(
      nil, -- nil key matches anything, timers use false keys
      t.pco.resume_all,
      timeout, -- wait for the time specified by the caller
      1) -- we only want this thread to wake up once
    data.handlers = data_handlers

    -- yield_all will yield this pco stack
    -- the callback will resume this stack
    local event_data
    repeat
      event_data = table.pack(t.pco.yield_all(timeout))
      -- during sleep, we may have been suspended
    until getmetatable(t).__status ~= "suspended"
    return table.unpack(event_data, 1, event_data.n)
  end

  t:attach()
  t.pco.resume_all(...) -- threads start out running

  return t
end

return thread
