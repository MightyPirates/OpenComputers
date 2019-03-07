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
computer.pullSignal = function(seconds) -- dispatch
  checkArg(1, seconds, "number", "nil")
  seconds = seconds or math.huge
  local uptime = computer.uptime
  local deadline = uptime() + seconds
  repeat
    local interrupting = uptime() - lastInterrupt > 1 and keyboard.isControlDown() and keyboard.isKeyDown(keyboard.keys.c)
    if interrupting then
      lastInterrupt = uptime()
      if keyboard.isAltDown() then
        require("process").info().data.signal("interrupted", 0)
        return
      end
      event.push("interrupted", lastInterrupt)
    end

    local closest = deadline
    for _,handler in pairs(handlers) do
      closest = math.min(handler.timeout, closest)
    end

    local event_data = table.pack(handlers(closest - uptime()))
    local signal = event_data[1]
    local copy = {}
    for id,handler in pairs(handlers) do
      copy[id] = handler
    end
    for id,handler in pairs(copy) do
      -- timers have false keys
      -- nil keys match anything
      if (handler.key == nil or handler.key == signal) or uptime() >= handler.timeout then
        handler.times = handler.times - 1
        handler.timeout = handler.timeout + handler.interval
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
    if signal then
      return table.unpack(event_data, 1, event_data.n)
    end
  until uptime() >= deadline
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

-------------------------------------------------------------------------------

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

function event.pullFiltered(...)
  local args = table.pack(...)
  local seconds, filter = math.huge

  if type(args[1]) == "function" then
    filter = args[1]
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "function", "nil")
    seconds = args[1]
    filter = args[2]
  end

  repeat
    local signal = table.pack(computer.pullSignal(seconds))
    if signal.n > 0 then
      if not (seconds or filter) or filter == nil or filter(table.unpack(signal, 1, signal.n)) then
        return table.unpack(signal, 1, signal.n)
      end
    end
  until signal.n == 0
end

-- users may expect to find event.push to exist
event.push = computer.pushSignal

require("package").delay(event, "/lib/core/full_event.lua")

-------------------------------------------------------------------------------

return event
