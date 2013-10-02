local function checkArg(n, have, ...)
  have = type(have)
  for _, want in pairs({...}) do
    if have == want then return end
  end
  error("bad argument #" .. n .. " (" .. table.concat({...}, " or ") ..
        " expected, got " .. have .. ")", 3)
end

-------------------------------------------------------------------------------

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

function event.listen(name, callback, weak)
  checkArg(2, callback, "function")
  table.insert(listenersFor(name, weak), callback)
end

function event.ignore(name, callback)
  local function remove(list)
    for k, v in ipairs(list) do
      if v == callback then
        table.remove(list, k)
        return
      end
    end
  end
  remove(listenersFor(name, false))
  remove(listenersFor(name, true))
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
    if info.after < os.clock() then
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

function event.timer(timeout, callback)
  local id = #timers + 1
  timers[id] = {after = os.clock() + timeout, callback = callback}
  return id
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

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  timers[timerId] = nil
end

--[[ Error handler for ALL event callbacks. If this returns a value,
     the computer will crash. Otherwise it'll keep going. ]]
function event.error(message)
  return message
end

function coroutine.sleep(seconds)
  checkArg(1, seconds, "number")
  local target = os.clock() + seconds
  repeat
    local closest = target
    for _, info in pairs(timers) do
      if info.after < closest then
        closest = info.after
      end
    end
    event.fire(os.signal(nil, closest - os.clock()))
  until os.clock() >= target
end
