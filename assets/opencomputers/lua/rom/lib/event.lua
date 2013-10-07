local listeners = {}
local weakListeners = {}
local timers = {}

local function listenersFor(name, weak)
  checkArg(1, name, "string")
  if weak then
    weakListeners[name] = weakListeners[name] or setmetatable({}, {__mode = "v"})
    return weakListeners[name]
  else
    listeners[name] = listeners[name] or {}
    return listeners[name]
  end
end

-------------------------------------------------------------------------------

event = {}

--[[ Error handler for ALL event callbacks. If this returns a value,
     the error will be rethrown, possibly leading to a computer crash. ]]
function event.error(message)
  return message
end

function event.fire(name, ...)
  -- We may have no arguments at all if the call is just used to drive the
  -- timer check (for example if we had no signal in coroutine.sleep()).
  if name then
    checkArg(1, name, "string")
    for _, callback in ipairs(listenersFor(name, false)) do
      local result, message = xpcall(callback, event.error, name, ...)
      if not result and message then
        error(message, 0)
      end
    end
    for _, callback in ipairs(listenersFor(name, true)) do
      local result, message = xpcall(callback, event.error, name, ...)
      if not result and message then
        error(message, 0)
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
    if not result and message then
      error(message, 0)
    end
  end
end

function event.ignore(name, callback)
  local function remove(list)
    for i = 1, #list do
      if list[i] == callback then
        table.remove(list, i)
        return
      end
    end
  end
  remove(listenersFor(name, false))
  remove(listenersFor(name, true))
end

function event.listen(name, callback, weak)
  checkArg(2, callback, "function")
  local list = listenersFor(name, weak)
  for i = 1, #list do
    if list[i] == callback then
      return
    end
  end
  table.insert(list, callback)
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

-------------------------------------------------------------------------------

function coroutine.sleep(seconds)
  seconds = seconds or math.huge
  checkArg(1, seconds, "number")
  local target = os.uptime() + seconds
  repeat
    local closest = target
    for _, info in pairs(timers) do
      if info.after < closest then
        closest = info.after
      end
    end
    event.fire(os.signal(nil, closest - os.uptime()))
  until os.uptime() >= (target == math.huge and 0 or target)
end
