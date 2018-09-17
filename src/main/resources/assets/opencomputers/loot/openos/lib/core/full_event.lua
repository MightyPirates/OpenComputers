local event = require("event")

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

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  if event.handlers[timerId] then
    event.handlers[timerId] = nil
    return true
  end
  return false
end

function event.ignore(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  for id, handler in pairs(event.handlers) do
    if handler.key == name and handler.callback == callback then
      event.handlers[id] = nil
      return true
    end
  end
  return false
end

function event.onError(message)
  local log = io.open("/tmp/event.log", "a")
  if log then
    pcall(log.write, log, tostring(message), "\n")
    log:close()
  end
end

function event.timer(interval, callback, times)
  checkArg(1, interval, "number")
  checkArg(2, callback, "function")
  checkArg(3, times, "number", "nil")
  return event.register(false, callback, interval, times)
end
