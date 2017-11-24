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
local box_thread_list = {close = thread.waitForAll}

local function get_process_threads(proc, bCreate)
  local handles = proc.data.handles
  for _,next_handle in ipairs(handles) do
    local handle_mt = getmetatable(next_handle)
    if handle_mt and handle_mt.__index == box_thread_list then
      return next_handle
    end
  end
  if bCreate then
    local btm = setmetatable({}, {__index = box_thread_list})
    table.insert(handles, btm)
    return btm
  end
end

function box_thread:resume()
  local mt = getmetatable(self)
  if mt.__status ~= "suspended" then
    return nil, "cannot resume " .. mt.__status .. " thread"
  end
  mt.__status = "running"
  -- register the thread to wake up
  if coroutine.status(self.pco.root) == "suspended" and not mt.reg then
    mt.register(0)
  end
  return true
end

function box_thread:suspend()
  local mt = getmetatable(self)
  if mt.__status ~= "running" then
    return nil, "cannot suspend " .. mt.__status .. " thread"
  end
  mt.__status = "suspended"
  local pco_status = coroutine.status(self.pco.root)
  if pco_status == "running" or pco_status == "normal" then
    mt.coma()
  end
  return true
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

  if mt.attached then
    local prev_threads = assert(get_process_threads(mt.attached), "thread panic: no thread handle")
    for index,t_in_list in ipairs(prev_threads) do
      if t_in_list == self then
        table.remove(prev_threads, index)
        break
      end
    end
  end

  -- registration happens on the attached proc, unregister before reparenting
  local waiting_handler = mt.unregister()

  -- attach to parent or the current process
  mt.attached = proc

  -- this process may not have a box_thread list
  local threads = get_process_threads(proc, true)
  table.insert(threads, self)

  -- register on the new parent
  if waiting_handler then -- event-waiting
    mt.register(waiting_handler.timeout - computer.uptime())
  end

  return self
end

function thread.current()
  local proc = process.findProcess()
  local thread_root
  while proc do
    if thread_root then
      for _,bt in ipairs(get_process_threads(proc) or {}) do
        if bt.pco.root == thread_root then
          return bt
        end
      end
    else
      thread_root = proc.data.coroutine_handler.root
    end
    proc = proc.parent
  end
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
    -- pullSignal will yield_past this point
    -- but yield will return here, we pullSignal from here to yield_past
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
    mt.unregister()
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
    mt.reg = mt.attached.data.handlers[mt.id]
  end

  function mt.unregister()
    local id = mt.id
    local reg = mt.reg
    mt.id = nil
    mt.reg = nil
    -- before just removing a handler, make sure it is still ours
    if id and mt.attached and mt.attached.data.handlers[id] == reg then
      mt.attached.data.handlers[id] = nil
      return reg
    end
  end

  function mt.coma()
    mt.unregister() -- we should not wake up again (until resumed)
    while mt.__status == "suspended" do
      t.pco.yield_past(t.pco.root, 0)
    end
  end

  function mt.process.data.pull(_, timeout)
    --[==[
    yield_past(root) will yield until out of this thread
    registration puts in a callback to resume this thread

    Subsequent registrations are necessary in case the thread is suspended
    This thread yields when suspended, entering a coma state
    -> coma state: yield without registration

    resume will regsiter a wakeup call, breaks coma

    subsequent yields need not specify a timeout because
    we already legitimately resumed only to find out we had been suspended

    3 places register for wake up
    1. computer.pullSignal [this path]
    2. t:attach(proc) will unregister and re-register
    3. t:resume() of a suspended thread
    ]==]
    mt.register(timeout)
    local event_data = table.pack(t.pco.yield_past(t.pco.root, timeout))
    mt.coma()
    return table.unpack(event_data, 1, event_data.n)
  end

  function mt.close()
    if t:status() == "dead" then
      return
    end
    local threads = get_process_threads(mt.attached)
    for index,t_in_list in ipairs(threads) do
      if t_in_list == t then
        table.remove(threads, index)
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
