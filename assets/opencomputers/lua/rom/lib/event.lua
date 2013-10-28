-- We track listeners per thread to allow killing them when their coroutine
-- dies / goes out of scope.
local threads = setmetatable({}, {__mode = "k"})
local closestTimer = 0 -- for event.wait to know how to sleep

local function listeners(create)
  local thread = coroutine.running()
  if not threads[thread] and create then
    threads[thread] = {listeners = {[false] = {}, [true] = {}}, timers = {}}
  end
  return threads[thread]
end

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
    local function listeners()
      -- Copy the listener lists because they may be changed by callbacks.
      local list = {}
      for thread, info in pairs(threads) do
        if coroutine.status(thread) == "dead" then
          threads[thread] = nil
        else
          for weak, listeners in pairs(info.listeners) do
            if weak then
              for name in pairs(listeners) do -- clean up weak tables
                if #listeners[name] == 0 then
                  listeners[name] = nil
                end
              end
            end
            if listeners[name] then
              for _, listener in ipairs(listeners[name]) do
                table.insert(list, listener)
              end
            end
          end
        end
      end
      return list
    end
    for _, callback in ipairs(listeners()) do
      call(callback, name, ...)
    end
  end
  local elapsed = {}
  for thread, info in pairs(threads) do
    if coroutine.status(thread) == "dead" then
      threads[thread] = nil
    else
      for id, timer in pairs(info.timers) do
        if timer.after <= os.uptime() then
          table.insert(elapsed, timer.callback)
          info.timers[id] = nil
        end
      end
    end
  end
  for _, callback in ipairs(elapsed) do
    call(callback)
  end
end

function event.ignore(name, callback, weak)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  weak = weak ~= nil and weak ~= false
  local list = listeners(false)
  if list and list.listeners[weak] and list.listeners[weak][name] then
    local listeners = list.listeners[weak][name]
    for i = 1, #listeners do
      if listeners[i] == callback then
        table.remove(listeners, i)
        if #listeners == 0 then
          list.listeners[weak][name] = nil
        end
        return true
      end
    end
  end
  return false
end

function event.listen(name, callback, weak)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  weak = weak ~= nil and weak ~= false
  local list = listeners(true).listeners[weak]
  if not list[name] then
    if weak then
      list[name] = setmetatable({}, {__mode = "v"})
    else
      list[name] = {}
    end
  end
  list = list[name]
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
  local list = listeners(false)
  if list and list.timers[timerId] then
    list.timers[timerId] = nil
    return true
  end
  return false
end

function event.interval(frequency, callback)
  local interval = {}
  local function onTimer()
    interval.id = event.timer(frequency, onTimer)
    callback()
  end
  interval.id = event.timer(frequency, onTimer)
  return interval
end

function event.timer(timeout, callback)
  local list, id = listeners(true).timers
  repeat
    id = math.floor(math.random(1, 0x7FFFFFFF))
  until not list[id]
  list[id] = {after = os.uptime() + timeout, callback = callback}
  if closestTimer < os.uptime() or closestTimer > list[id].after then
    closestTimer = list[id].after
  end
  return id
end

function event.wait(filter, seconds)
  checkArg(1, filter, "string", "nil")
  seconds = seconds or (filter and math.huge or 0/0)
  checkArg(2, seconds, "number")
  local function isNaN(n) return n ~= n end
  local deadline = os.uptime() + (isNaN(seconds) and 0 or seconds)
  repeat
    local closest = math.min(closestTimer, isNaN(seconds) and math.huge or deadline)
    local signal = table.pack(os.signal(nil, closest - os.uptime()))
    event.fire(table.unpack(signal, 1, signal.n))
    if filter and type(signal[1]) == "string" and signal[1]:match(filter) then
      return table.unpack(signal, 1, signal.n)
    end
  until os.uptime() >= deadline
end
