local computer = require("computer")
local keyboard = require("keyboard")

local event = {}
local handlers = {}
local lastInterrupt = -math.huge

event.handlers = handlers

function event.register(key, callback, interval, times, opt_handlers)
  local handler =
  {
    key = key,
    times = times or 1,
    callback = callback,
    interval = interval or math.huge,
  }

  handler.timeout = computer.uptime() + handler.interval
  opt_handlers = opt_handlers or handlers

  local id = 0
  repeat
    id = id + 1
  until not opt_handlers[id]

  opt_handlers[id] = handler
  return id
end

local _pullSignal = computer.pullSignal
setmetatable(handlers, {__call=function(_,...)return _pullSignal(...)end})
computer.pullSignal = function(...) -- dispatch
  local current_time = computer.uptime()
  local interrupting = current_time - lastInterrupt > 1 and keyboard.isControlDown() and keyboard.isKeyDown(keyboard.keys.c)
  if interrupting then
    lastInterrupt = current_time
    if keyboard.isAltDown() then
      error("interrupted", 0)
    end
    event.push("interrupted", current_time)
  end
  local event_data = table.pack(handlers(...))
  local signal = event_data[1]
  local copy = {}
  for id,handler in pairs(handlers) do
    copy[id] = handler
  end
  for id,handler in pairs(copy) do
    -- timers have false keys
    -- nil keys match anything
    if (handler.key == nil or handler.key == signal) or current_time >= handler.timeout then
      handler.times = handler.times - 1
      handler.timeout = current_time + handler.interval
      -- we have to remove handlers before making the callback in case of timers that pull
      -- and we have to check handlers[id] == handler because callbacks may have unregistered things
      if handler.times <= 0 and handlers[id] == handler then
        handlers[id] = nil
      end
      -- call
      local result, message = pcall(handler.callback, table.unpack(event_data, 1, event_data.n))
      if not result then
        pcall(event.onError, message)
      elseif message == false and handlers[id] == handler then
        handlers[id] = nil
      end
    end
  end
  return table.unpack(event_data, 1, event_data.n)
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
  for _, handler in pairs(handlers) do
    if handler.key == name and handler.callback == callback then
      return false
    end
  end
  return event.register(name, callback, math.huge, math.huge)
end

function event.onError(message)
  local log = io.open("/tmp/event.log", "a")
  if log then
    pcall(log.write, log, message, "\n")
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
    local closest = deadline
    for _,handler in pairs(handlers) do
      closest = math.min(handler.timeout, closest)
    end
    local signal = table.pack(computer.pullSignal(closest - computer.uptime()))
    if signal.n > 0 then
      if not (seconds or filter) or filter == nil or filter(table.unpack(signal, 1, signal.n)) then
        return table.unpack(signal, 1, signal.n)
      end
    end
  until computer.uptime() >= deadline
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
