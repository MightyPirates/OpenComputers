local pipe = require("pipe")
local event = require("event")
local process = require("process")
local computer = require("computer")

local thread = {}
local init_thread

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
      local mt = getmetatable(t)
      local result = mt.attached.data.result
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
  return waitForDeath({self}, timeout, true)
end

function box_thread:kill()
  getmetatable(self).close()
end

function box_thread:detach()
  return self:attach(init_thread)
end

function box_thread:attach(parent)
  checkArg(1, parent, "thread", "number", "nil")
  local mt = assert(getmetatable(self), "thread panic: no metadata")
  local proc = process.info(parent)
  if not proc then return nil, "thread failed to attach, process not found" end
  if mt.attached == proc then return self end -- already attached

  local waiting_handler
  if mt.attached then
    local prev_btHandle = assert(get_box_thread_handle(mt.attached.data.handles), "thread panic: no thread handle")
    for i,h in ipairs(prev_btHandle) do
      if h == self then
        table.remove(prev_btHandle, i)
        if mt.id then
          waiting_handler = assert(mt.attached.data.handlers[mt.id], "thread panic: no event handler")
          mt.attached.data.handlers[mt.id] = nil
        end
        break
      end
    end
  end

  -- attach to parent or the current process
  mt.attached = proc
  local handles = proc.data.handles

  -- this process may not have a box_thread manager handle
  local btHandle = get_box_thread_handle(handles, true)
  table.insert(btHandle, self)

  if waiting_handler then -- event-waiting
    mt.register(waiting_handler.timeout - computer.uptime())
  end

  return self
end

function thread.create(fp, ...)
  checkArg(1, fp, "function")

  local t = {}
  local mt = {__status="suspended",__index=box_thread}
  setmetatable(t, mt)
  t.pco = pipe.createCoroutineStack(function(...)
    mt.__status = "running"
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
  end, nil, "thread")

  --special resume to keep track of process death
  function mt.private_resume(...)
    mt.id = nil
    -- this thread may have been killed
    if t:status() == "dead" then return end
    local result = table.pack(t.pco.resume(t.pco.root, ...))
    if t.pco.status(t.pco.root) == "dead" then
      mt.close()
    end
    return table.unpack(result, 1, result.n)
  end

  mt.process = process.list[t.pco.root]
  mt.process.data.handlers = {}

  function mt.register(timeout)
    -- register a timeout handler
    mt.id = event.register(
      nil, -- nil key matches anything, timers use false keys
      mt.private_resume,
      timeout, -- wait for the time specified by the caller
      1, -- we only want this thread to wake up once
      mt.attached.data.handlers) -- optional arg, to specify our own handlers
  end

  function mt.process.data.pull(_, timeout)
    mt.register(timeout)
    -- yield_past(root) will yield until out of this thread
    -- the callback will resume this stack
    local event_data
    repeat
      event_data = table.pack(t.pco.yield_past(t.pco.root, timeout))
      -- during sleep, we may have been suspended
    until t:status() ~= "suspended"
    return table.unpack(event_data, 1, event_data.n)
  end

  function mt.close()
    if t:status() == "dead" then
      return
    end
    local htm = get_box_thread_handle(mt.attached.data.handles)
    for _,ht in ipairs(htm) do
      if ht == t then
        table.remove(htm, _)
        break
      end
    end
    mt.__status = "dead"
    event.push("thread_exit")
  end

  t:attach() -- the current process
  mt.private_resume(...) -- threads start out running

  return t
end

do
  local handlers = event.handlers
  local handlers_mt = getmetatable(handlers)
  -- the event library sets a metatable on handlers, but we set threaded=true
  if not handlers_mt.threaded then
    -- find the root process
    local root_data
    for t,p in pairs(process.list) do
      if not p.parent then
        init_thread = t
        root_data = p.data
        break
      end
    end
    assert(init_thread, "thread library panic: no init thread")
    handlers_mt.threaded = true
    -- handles might be optimized out for memory
    root_data.handles = root_data.handles or {}
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

return thread
