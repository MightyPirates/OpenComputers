local listeners, timers = {}, {}

-------------------------------------------------------------------------------

event = {}

--[[ Error handler for ALL event callbacks. If this throws an error or is not,
     set the computer will immediately shut down. ]]
function event.error(message)
  local log = io.open("tmp/event.log", "a")
  if log then
    log:write(message .. "\n")
    log:close()
  end
end

function event.fire(name, ...)
  local function call(callback, ...)
    local result, message = pcall(callback, ...)
    if not result and not (event.error and pcall(event.error, message)) then
      os.shutdown()
    end
  end
  -- We may have no arguments at all if the call is just used to drive the
  -- timer check (for example if we had no signal in event.wait()).
  if name then
    checkArg(1, name, "string")
    if listeners[name] then
      local function callbacks()
        local list = {}
        for index, listener in ipairs(listeners[name]) do
          list[index] = listener
        end
        return list
      end
      for _, callback in ipairs(callbacks()) do
        call(callback, name, ...)
      end
    end
  end
  local function elapsed()
    local list = {}
    for id, timer in pairs(timers) do
      if timer.after <= os.uptime() then
        table.insert(list, timer.callback)
        timer.times = timer.times - 1
        if timer.times <= 0 then
          timers[id] = nil
        else
          timer.after = timer.after + timer.interval
        end
      end
    end
    return list
  end
  for _, callback in ipairs(elapsed()) do
    call(callback)
  end
end

function event.ignore(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  if listeners[name] then
    for i = 1, #listeners[name] do
      if listeners[name][i] == callback then
        table.remove(listeners[name], i)
        if #listeners[name] == 0 then
          list.listeners[name] = nil
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

-------------------------------------------------------------------------------

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  if timers[timerId] then
    timers[timerId] = nil
    return true
  end
  return false
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
    after = os.uptime() + interval,
    callback = callback,
    times = times or 1
  }
  return id
end

-------------------------------------------------------------------------------

function event.wait(seconds, name, ...)
  checkArg(1, seconds, "number", "nil")
  checkArg(2, name, "string", "nil")
  local filter = table.pack(...)
  local hasFilter = name ~= nil
  for i = 1, filter.n do
    hasFilter = hasFilter or filter[i] ~= nil
  end

  local function matches(signal)
    if not (not name or type(signal[1]) == "string" and signal[1]:match(name)) then
      return false
    end
    for i = 1, filter.n do
      if filter[i] ~= nil and filter[i] ~= signal[i + 1] then
        return false
      end
    end
    return true
  end

  local deadline = seconds and
                   (os.uptime() + seconds) or
                   (hasFilter and math.huge or 0)
  repeat
    local closest = seconds and deadline or math.huge
    for _, timer in pairs(timers) do
      closest = math.min(closest, timer.after)
    end
    local signal = table.pack(os.signal(nil, closest - os.uptime()))
    event.fire(table.unpack(signal, 1, signal.n))
    if event.shouldInterrupt() then
      error("interrupted", 0)
    end
    if not (seconds or hasFilter) or matches(signal) then
      return table.unpack(signal, 1, signal.n)
    end
  until os.uptime() >= deadline
end

function event.shouldInterrupt()
  return keyboard.isControlDown() and
         keyboard.isAltDown() and
         keyboard.isKeyDown(keyboard.keys.c)
end
