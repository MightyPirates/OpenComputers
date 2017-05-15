local computer = require("computer")
local keyboard = require("keyboard")

local event = {}
local handlers = {}
local lastInterrupt = -math.huge

event.handlers = handlers

local function call(callback, ...)
  local result, message = pcall(callback, ...)
  if not result and type(event.onError) == "function" then
    pcall(event.onError, message)
    return
  end
  return message
end

function event.register(key, callback, interval, times)
  local handler =
  {
    key = key,
    times = times or 1,
    callback = callback,
    interval = interval or math.huge,
  }

  handler.timeout = computer.uptime() + handler.interval

  local id = 0
  repeat
    id = id + 1
  until not handlers[id]

  handlers[id] = handler
  return id
end

local function time_to_nearest()
  local timeout = math.huge
  for _,handler in pairs(handlers) do
    if timeout > handler.timeout then
      timeout = handler.timeout
    end
  end
  return timeout
end

local function dispatch(...)
  local signal = (...)
  local eligable = {}
  local ids_to_remove = {}
  local time = computer.uptime()
  for id, handler in pairs(handlers) do
    -- timers have false keys
    -- nil keys match anything
    local key = handler.key
    key = (key == nil and signal) or key
    if (signal and key == signal) or time >= handler.timeout then
      -- push ticks to end of list (might be slightly faster to fire them last)
      table.insert(eligable, select(handler.key and 1 or 2, 1, {handler.callback, id}))

      handler.times = handler.times - 1
      handler.timeout = computer.uptime() + handler.interval
      if handler.times <= 0 then
        table.insert(ids_to_remove, id)
      end
    end
  end
  for _,pack in ipairs(eligable) do
    if call(pack[1], ...) == false then
      table.insert(ids_to_remove, pack[2])
    end
  end
  for _,id in ipairs(ids_to_remove) do
    handlers[id] = nil
  end
end

local _pullSignal = computer.pullSignal
computer.pullSignal = function(...)
  return (function(...)
    dispatch(...)
    return ...
  end)(_pullSignal(...))
end

local function createPlainFilter(name, ...)
  local filter = table.pack(...)
  if name == nil and filter.n == 0 then
    return nil
  end

  return function(...)
    local signal = table.pack(...)
    if name and not (type(signal[1]) == "string" and signal[1]:match(name)) then
      return false
    end
    for i = 1, filter.n do
      if filter[i] ~= nil and filter[i] ~= signal[i + 1] then
        return false
      end
    end
    return true
  end
end

local function createMultipleFilter(...)
  local filter = table.pack(...)
  if filter.n == 0 then
    return nil
  end

  return function(...)
    local signal = table.pack(...)
    if type(signal[1]) ~= "string" then
      return false
    end
    for i = 1, filter.n do
      if filter[i] ~= nil and signal[1]:match(filter[i]) then
        return true
      end
    end
    return false
  end
end
-------------------------------------------------------------------------------

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  if handlers[timerId] then
    handlers[timerId] = nil
    return true
  end
  return false
end

function event.ignore(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  for id, handler in pairs(handlers) do
    if handler.key == name and handler.callback == callback then
      handlers[id] = nil
      return true
    end
  end
  return false
end

function event.listen(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  for id, handler in pairs(handlers) do
    if handler.key == name and handler.callback == callback then
      return false
    end
  end
  return event.register(name, callback, math.huge, math.huge)
end

function event.onError(message)
  local log = io.open("/tmp/event.log", "a")
  if log then
    log:write(message .. "\n")
    log:close()
  end
end

function event.pull(...)
  local args = table.pack(...)
  if type(args[1]) == "string" then
    return event.pullFiltered(createPlainFilter(...))
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "string", "nil")
    return event.pullFiltered(args[1], createPlainFilter(select(2, ...)))
  end
end

function event.pullMultiple(...)
  local seconds
  local args
  if type(...) == "number" then
    seconds = ...
    args = table.pack(select(2,...))
    for i=1,args.n do
      checkArg(i+1, args[i], "string", "nil")
    end
  else
    args = table.pack(...)
    for i=1,args.n do
      checkArg(i, args[i], "string", "nil")
    end
  end
  return event.pullFiltered(seconds, createMultipleFilter(table.unpack(args, 1, args.n)))
end

function event.pullFiltered(...)
  local args = table.pack(...)
  local seconds, filter

  if type(args[1]) == "function" then
    filter = args[1]
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "function", "nil")
    seconds = args[1]
    filter = args[2]
  end

  local deadline = seconds and (computer.uptime() + seconds) or math.huge
  repeat
    local closest = math.min(deadline, time_to_nearest())
    local signal = table.pack(computer.pullSignal(closest - computer.uptime()))
    if event.shouldInterrupt() then
      lastInterrupt = computer.uptime()
      error("interrupted", 0)
    end
    if event.shouldSoftInterrupt() and (filter == nil or filter("interrupted", computer.uptime() - lastInterrupt))  then
      local awaited = computer.uptime() - lastInterrupt
      lastInterrupt = computer.uptime()
      return "interrupted", awaited
    end
    if signal.n > 0 then
      if not (seconds or filter) or filter == nil or filter(table.unpack(signal, 1, signal.n)) then
        return table.unpack(signal, 1, signal.n)
      end
    end
  until computer.uptime() >= deadline
end

function event.shouldInterrupt()
  return computer.uptime() - lastInterrupt > 1 and
         keyboard.isControlDown() and
         keyboard.isAltDown() and
         keyboard.isKeyDown(keyboard.keys.c)
end

function event.shouldSoftInterrupt()
  return computer.uptime() - lastInterrupt > 1 and
         keyboard.isControlDown() and
         keyboard.isKeyDown(keyboard.keys.c)
end

function event.timer(interval, callback, times)
  checkArg(1, interval, "number")
  checkArg(2, callback, "function")
  checkArg(3, times, "number", "nil")
  return event.register(false, callback, interval, times)
end

-- users may expect to find event.push to exist
event.push = computer.pushSignal

-------------------------------------------------------------------------------

return event
