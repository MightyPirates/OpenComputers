local listeners = {}
local weakListeners = {}
local timers = {}

local function listenersFor(name, weak, create)
  checkArg(1, name, "string")
  if weak then
    if not weakListeners[name] and create then
      weakListeners[name] = weakListeners[name] or setmetatable({}, {__mode = "v"})
    end
    return weakListeners[name] or {}
  else
    if not listeners[name] and create then
      listeners[name] = listeners[name] or {}
    end
    return listeners[name] or {}
  end
end

-------------------------------------------------------------------------------

event = {}

--[[ Error handler for ALL event callbacks. If this returns a value,
     the error will be rethrown, possibly leading to a computer crash. ]]
function event.error(message)
  return debug.traceback(message)
end

function event.fire(name, ...)
  -- We may have no arguments at all if the call is just used to drive the
  -- timer check (for example if we had no signal in event.wait()).
  if name then
    checkArg(1, name, "string")
    local function copy(listA, listB)
      local result = {}
      for _, v in ipairs(listA) do table.insert(result, v) end
      for _, v in ipairs(listB) do table.insert(result, v) end
      return result
    end
    -- Copy the listener lists because they may be changed by callbacks.
    local listeners = copy(listenersFor(name, false), listenersFor(name, true))
    for _, callback in ipairs(listeners) do
      local result, message = xpcall(callback, event.error, name, ...)
      if not result and message then -- only if handler returned something.
        error(message, 0)
      elseif result and message == false then
        break
      end
    end
  end
  local elapsed = {}
  for id, info in pairs(timers) do
    if info.after < os.uptime() then
      table.insert(elapsed, info.callback)
      timers[id] = nil
    end
  end
  for _, callback in ipairs(elapsed) do
    local result, message = xpcall(callback, event.error)
    if not result and message then -- only if handler returned something.
      error(message, 0)
    end
  end
end

function event.ignore(name, callback, weak)
  checkArg(2, callback, "function")
  local list = listenersFor(name, weak)
  for i = 1, #list do
    if list[i] == callback then
      table.remove(list, i)
      return true
    end
  end
  return false
end

function event.listen(name, callback, weak)
  checkArg(2, callback, "function")
  local list = listenersFor(name, weak, true)
  for i = 1, #list do
    if list[i] == callback then
      return false
    end
  end
  table.insert(list, callback)
  return true
end

-------------------------------------------------------------------------------

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  timers[timerId] = nil
end

function event.interval(timeout, callback)
  local interval = {}
  local function onTimer()
    interval.id = event.timer(timeout, onTimer)
    callback()
  end
  interval.id = event.timer(timeout, onTimer)
  return interval
end

function event.timer(timeout, callback)
  local id
  repeat
    id = math.floor(math.random(1, 0x7FFFFFFF))
  until not timers[id]
  timers[id] = {after = os.uptime() + timeout, callback = callback}
  return id
end

function event.wait(seconds)
  seconds = seconds or 0/0
  checkArg(1, seconds, "number")
  local function isNaN(n) return n ~= n end
  local target = os.uptime() + (isNaN(seconds) and 0 or seconds)
  repeat
    local closest = isNaN(seconds) and math.huge or target
    for _, info in pairs(timers) do
      if info.after < closest then
        closest = info.after
      end
    end
    event.fire(os.signal(nil, closest - os.uptime()))
  until os.uptime() >= target
end
