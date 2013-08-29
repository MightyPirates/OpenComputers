--[[
  Basic OS functionality, such as launching new programs and loading drivers.

  This is called as the main coroutine by the computer. If this throws, the
  computer crashes. If this returns, the computer is considered powered off.
]]

local function main()
  --[[
    A copy the globals table to avoid user-space programs messing with us. The
    actual copy is created below, because we first need to declare the table copy
    function... which in turn uses this variable to avoid being tampered with.
  ]]
  local g = _G

  -- List of all active processes.
  local processes = {}

  -- The ID of the process currently running.
  local currentProcess = 0

  --[[
    Returns the process ID of the currently executing process.
  ]]
  function _G.os.pid()
    return currentProcess
  end

  -- Starts a new process using the specified callback.
  function _G.os.execute(task)
    local callback = task
    if g.type(task) == "string" then
      -- Check if we have a file system, load script and set callback.
      -- TODO ...
      g.setfenv(callback, g.setmetatable({}, {__index = _G}))
    end
    g.table.insert(processes, {
      pid = #processes,
      thread = g.coroutine.create(callback),
      parent = currentProcess,
      sleep = 0,
      signals = {}
    })
  end

  --[[ Stops the process currently being executed. ]]
  function _G.os.exit()
    g.coroutine.yield("terminate")
  end

  --[[ Makes the current process sleep for the specified amount of time. ]]
  function _G.os.sleep(seconds)
    assert(g.type(seconds) == "number",
           g.string.format("'number' expected, got '%s'", g.type(seconds)))
    processes[currentProcess].sleep = g.os.clock() + seconds
    while processes[currentProcess].sleep > g.os.clock() do
      local signal = {g.coroutine.yield()}
      if signal[1] then
        processes[currentProcess][signal[1]](g.unpack(signal))
      end
    end
  end

  --[[ Registers or unregisters a callback for a signal. ]]
  function _G.os.signal(name, callback)
    assert(g.type(name) == "string" and g.type(callback) == "function",
           g.string.format("'string', 'function' expected, got '%s', '%s'",
                           g.type(name), g.type(callback)))
    processes[currentProcess][name] = callback
  end

  -- We replace the default yield function so that be can differentiate between
  -- process level yields and coroutine level yields - in case a process spawns
  -- new coroutines.
  --[[
  function _G.coroutine.yield(...)
    while true do
      local result = {g.coroutine.yield(nil, ...)}
      if result[1] == "signal" then
      end
    end
  end

  function _G.coroutine.resume(...)
    while true do
      local result = {g.coroutine.resume(...)}
      if result[1] and result[2] == "signal" then

      else
        return result[1], g.select(3, g.unpack(result))
      end
    end
  end
  ]]

  -- Create the actual copy now that we have our copying function.
  g = g.table.copy(g, true)

  -- Spawn the init process, which is basically a Lua interpreter.
  g.os.execute(function() print("hi") os.sleep(5) end)

  print("Running kernel...")

  while true do
    print("ping")
    g.coroutine.yield(5)
  end

  -- Begin running our processes. We run all processes consecutively if they are
  -- currently "awake", meaning not waiting for a call to os.sleep to return. If
  -- a signal arrives and the process has a callback for it, it is still resumed,
  -- though, to call the callback.
  -- If all processes are asleep, we yield the minimum sleep time, so that we can
  -- avoid busy waiting (resuming the main coroutine over and over again).
  local sleep = 0
  while #processes > 0 do
    local signal = {g.coroutine.yield(sleep)}
    local signalPid = signal[1]
    local signalName = signal[2]
    local signalArgs = g.select(3, g.unpack(signal))

    for _, process in ipairs(processes) do
      local awake = g.os.clock() >= process.sleep
      local target = process.signals[signalName] and
                     (signalPid < 1 or signalPid == process.pid)
      if awake or target then
        currentProcess = process.pid
        local result, cause = g.coroutine.resume(process.thread, "signal", signalName, g.unpack(signalArgs))
        if not result or g.coroutine.status(process.thread) == "dead" then
          process.thread = nil
        elseif cause == "terminate" then
          process.thread = nil
        end
      end
    end
    sleep = g.math.huge
    for i = #processes, 1, -1 do
      if processes[i].thread == nil then
        g.table.remove(processes, i)
      else
        sleep = g.math.min(processes[i].sleep, sleep)
      end
    end
    sleep = g.math.max(0, sleep - g.os.clock())
  end
end

-- local result, message = pcall1(function()
local result, message = pcall(main)
-- if not result then error(message) end end)

if not result then
  print(message)
end
return result, message