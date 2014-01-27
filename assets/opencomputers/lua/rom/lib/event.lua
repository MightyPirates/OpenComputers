local computer = require("computer")
local keyboard = require("keyboard")

local event, listeners, timers = {}, {}, {}
local lastInterrupt = -math.huge

local function matches(signal, name, filter)
  if name and not (type(signal[1]) == "string" and signal[1]:match(name))
  then
    return false
  end
  for i = 1, filter.n do
    if filter[i] ~= nil and filter[i] ~= signal[i + 1] then
      return false
    end
  end
  return true
end

local function call(callback, ...)
  local result, message = pcall(callback, ...)
  if not result and type(event.onError) == "function" then
    pcall(event.onError, message)
    return
  end
  return message
end

local function dispatch(signal, ...)
  if listeners[signal] then
    local function callbacks()
      local list = {}
      for index, listener in ipairs(listeners[signal]) do
        list[index] = listener
      end
      return list
    end
    for _, callback in ipairs(callbacks()) do
      if call(callback, signal, ...) == false then
        event.ignore(signal, callback) -- alternative method of removing a listener
      end
    end
  end
end

local function tick()
  local function elapsed()
    local list = {}
    for id, timer in pairs(timers) do
      if timer.after <= computer.uptime() then
        table.insert(list, timer.callback)
        timer.times = timer.times - 1
        if timer.times <= 0 then
          timers[id] = nil
        else
          timer.after = computer.uptime() + timer.interval
        end
      end
    end
    return list
  end
  for _, callback in ipairs(elapsed()) do
    call(callback)
  end
end

-------------------------------------------------------------------------------

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  if timers[timerId] then
    timers[timerId] = nil
    return true
  end
  return false
end

function event.ignore(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  if listeners[name] then
    for i = 1, #listeners[name] do
      if listeners[name][i] == callback then
        table.remove(listeners[name], i)
        if #listeners[name] == 0 then
          listeners[name] = nil
        end
        return true
      end
    end
  end
  return false
end

function event.listen(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  if listeners[name] then
    for i = 1, #listeners[name] do
      if listeners[name][i] == callback then
        return false
      end
    end
  else
    listeners[name] = {}
  end
  table.insert(listeners[name], callback)
  return true
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
  local seconds, name, filter
  if type(args[1]) == "string" then
    name = args[1]
    filter = table.pack(table.unpack(args, 2, args.n))
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "string", "nil")
    seconds = args[1]
    name = args[2]
    filter = table.pack(table.unpack(args, 3, args.n))
  end

  local hasFilter = name ~= nil
  if not hasFilter then
    for i = 1, filter.n do
      hasFilter = hasFilter or filter[i] ~= nil
    end
  end

  local deadline = seconds and
                   (computer.uptime() + seconds) or
                   (hasFilter and math.huge or 0)
  repeat
    local closest = seconds and deadline or math.huge
    for _, timer in pairs(timers) do
      closest = math.min(closest, timer.after)
    end
    local signal = table.pack(computer.pullSignal(closest - computer.uptime()))
    if signal.n > 0 then
      dispatch(table.unpack(signal, 1, signal.n))
    end
    tick()
    if event.shouldInterrupt() then
      lastInterrupt = computer.uptime()
      error("interrupted", 0)
    end
    if not (seconds or hasFilter) or matches(signal, name, filter) then
      return table.unpack(signal, 1, signal.n)
    end
  until computer.uptime() >= deadline
end

function event.shouldInterrupt()
  return computer.uptime() - lastInterrupt > 1 and
         keyboard.isControlDown() and
         keyboard.isAltDown() and
         keyboard.isKeyDown(keyboard.keys.c)
end

function event.timer(interval, callback, times)
  checkArg(1, interval, "number")
  checkArg(2, callback, "function")
  checkArg(3, times, "number", "nil")
  local id
  repeat
    id = math.floor(math.random(1, 0x7FFFFFFF))
  until not timers[id]
  timers[id] = {
    interval = interval,
    after = computer.uptime() + interval,
    callback = callback,
    times = times or 1
  }
  return id
end

-------------------------------------------------------------------------------

return event
