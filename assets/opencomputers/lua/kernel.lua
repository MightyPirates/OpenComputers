--[[ Basic functionality, drives userland and enforces timeouts.

     This is called as the main coroutine by the computer. If this throws, the
     computer crashes. If this returns, the computer is considered powered off.
--]]

--[[ Will be adjusted by the kernel when running, represents how long we can
     continue running without yielding. Used in the debug hook that enforces
     this timeout by throwing an error if it's exceeded. ]]
local deadline = 0

--[[ The hook installed for process coroutines enforcing the timeout. ]]
local function timeoutHook()
  local now = os.clock()
  if now > deadline then
    error({timeout=debug.traceback(2)}, 0)
  end
end

--[[ Set up the global environment we make available to userland programs. ]]
local function buildSandbox()
  local sandbox = {
    -- Top level values. The selection of kept methods rougly follows the list
    -- as available on the Lua wiki here: http://lua-users.org/wiki/SandBoxes
    assert = assert,
    error = error,
    pcall = pcall,
    xpcall = xpcall,

    ipairs = ipairs,
    next = next,
    pairs = pairs,

    rawequal = rawequal,
    rawget = rawget,
    rawlen = rawlen,
    rawset = rawset,

    select = select,
    type = type,
    tonumber = tonumber,
    tostring = tostring,

    -- We don't care what users do with metatables. The only raised concern was
    -- about breaking an environment, and we don't care about that.
    getmetatable = getmetatable,
    setmetatable = setmetatable,

    -- Custom print that actually writes to the screen buffer.
    print = print,

    bit32 = {
      arshift = bit32.arshift,
      band = bit32.band,
      bnot = bit32.bnot,
      bor = bit32.bor,
      btest = bit32.btest,
      bxor = bit32.bxor,
      extract = bit32.extract,
      replace = bit32.replace,
      lrotate = bit32.lrotate,
      lshift = bit32.lshift,
      rrotate = bit32.rrotate,
      rshift = bit32.rshift
    },

    coroutine = {
      create = coroutine.create,
      resume = coroutine.resume,
      running = coroutine.running,
      status = coroutine.status,
      wrap = coroutine.wrap,
      yield = coroutine.yield
    },

    math = {
      abs = math.abs,
      acos = math.acos,
      asin = math.asin,
      atan = math.atan,
      atan2 = math.atan2,
      ceil = math.ceil,
      cos = math.cos,
      cosh = math.cosh,
      deg = math.deg,
      exp = math.exp,
      floor = math.floor,
      fmod = math.fmod,
      frexp = math.frexp,
      huge = math.huge,
      ldexp = math.ldexp,
      log = math.log,
      max = math.max,
      min = math.min,
      modf = math.modf,
      pi = math.pi,
      pow = math.pow,
      rad = math.rad,
      random = math.random,
      randomseed = math.randomseed,
      sin = math.sin,
      sinh = math.sinh,
      sqrt = math.sqrt,
      tan = math.tan,
      tanh = math.tanh
    },

    os = {
      clock = os.clock,
      date = os.date,
      difftime = os.difftime,
      time = os.time,
      freeMemory = os.freeMemory,
      totalMemory = os.totalMemory
    },

    string = {
      byte = string.byte,
      char = string.char,
      dump = string.dump,
      find = string.find,
      format = string.format,
      gmatch = string.gmatch,
      gsub = string.gsub,
      len = string.len,
      lower = string.lower,
      match = string.match,
      rep = string.rep,
      reverse = string.reverse,
      sub = string.sub,
      upper = string.upper
    },

    table = {
      concat = table.concat,
      insert = table.insert,
      pack = table.pack,
      remove = table.remove,
      sort = table.sort,
      unpack = unpack,
      -- Custom functions.
      copy = table.copy,
      asreadonly = table.asreadonly,
      isreadonly = table.isreadonly
    }
  }
  sandbox._G = sandbox

  -- Allow sandboxes to load code, but only in text form, and in the sandbox.
  -- Note that we allow passing a custom environment, because if this is called
  -- from inside the sandbox, env must already be in the sandbox.
  function sandbox.load(code, env)
    return load(code, nil, "t", env or sandbox)
  end

  -- Make methods respect the read-only aspect of tables.
  do
    local function checkreadonly(t)
      if table.isreadonly(t) then
        error("trying to modify read-only table", 3)
      end
    end
    function sandbox.rawset(t, k, v)
      checkreadonly(t)
      rawset(t, k, v)
    end
    function sandbox.table.insert(t, k, v)
      checkreadonly(t)
      table.insert(t, k, v)
    end
    function sandbox.table.remove(t, k)
      checkreadonly(t)
      table.remove(t, k)
    end
    function sandbox.table.sort(t, f)
      checkreadonly(t)
      table.sort(t, f)
    end
  end

  --[[ Error thrower available to the userland. Used to differentiate system
       errors from user errors, such as timeouts (we rethrow system errors).
  --]]
  function sandbox.error(message, level)
    level = math.max(0, level or 1)
    error({message=message}, level > 0 and level + 1 or 0)
  end

  local function checkResult(success, result, ...)
    if success then
      return success, result, ...
    end
    if result.timeout then
      error({timeout=result.timeout .. "\n" .. debug.traceback(2)}, 0)
    end
    return success, result.message
  end

  function sandbox.pcall(f, ...)
    return checkResult(pcall(f, ...))
  end

  function sandbox.xpcall(f, msgh, ...)
    function handler(msg)
      return msg.message and {message=msgh(msg.message)} or msg
    end
    return checkResult(xpcall(f, handler, ...))
  end

  --[[ Install wrappers for coroutine management that reserves the first value
       returned by yields for internal stuff.
  --]]
  function sandbox.coroutine.yield(...)
    return coroutine.yield(nil, ...)
  end
  function sandbox.coroutine.resume(co, ...)
    if not debug.gethook(co) then -- Don't reset counter.
      debug.sethook(co, timeoutHook, "", 10000)
    end
    local result = {checkResult(coroutine.resume(co, ...))}
    if result[1] and result[2] then
      -- Internal yield, bubble up to the top and resume where we left off.
      return coroutine.resume(co, coroutine.yield(result[2]))
    end
    -- Userland yield or error, just pass it on.
    return result[1], select(3, table.unpack(result))
  end

  --[[ Suspends the computer for the specified amount of time. Note that
       signal handlers will still be called if a signal arrives.
  --]]
  function sandbox.os.sleep(seconds)
    checkType(1, seconds, "number")
    local target = os.clock() + seconds
    while os.clock() < target do
      -- Yielding a number here will tell the host it can wait with running us
      -- again for that long. Note that this is *not* a sleep! We may be resumed
      -- way sooner, e.g. because of signals or a state load (after an unload).
      -- That's why we put a loop around the thing.
      coroutine.yield(seconds)
    end
  end

  return sandbox
end

local function main()
  --[[ Create the sandbox as a thread-local variable so it is persisted. ]]
  local sandbox = buildSandbox()

  --[[ List of signal handlers, by name. ]]
  local signals = setmetatable({}, {__mode = "v"})

  --[[ Registers or unregisters a callback for a signal. ]]
  function sandbox.os.signal(name, callback)
    checkType(1, name, "string")
    checkType(2, callback, "function", "nil")

    local oldCallback = signals[name]
    signals[name] = callback
    return oldCallback
  end

  --[[ Error handler for signal callbacks. ]]
  local function onSignalError(msg)
    if type(msg) == "table" then
      if msg.timeout then
        msg = "too long without yielding"
      else
        msg = msg.message
      end
    end
    print(msg)
    return msg
  end

  --[[ Set up the shell, which is really just a Lua interpreter. ]]
  local shellThread
  local function startShell(safeMode)
    -- Set sandbox environment and create the shell runner.
    local _ENV = sandbox
    local function shell()
      function test(arg)
        print(string.format("%d SIGNAL! Available RAM: %d/%d",
          os.time(), os.freeMemory(), os.totalMemory()))
      end
      os.signal("test", test)

      local i = 0
      while true do
        i = i + 1
        print("ping " .. i)
        os.sleep(1)
      end
    end
    shellThread = coroutine.create(shell)
  end
  startShell()

  print("Running kernel...")

  -- Pending signal to be processed. We only either process a signal *or* run
  -- the shell, to keep the chance of long blocks low (and "accidentally" going
  -- over the timeout).
  local signal
  while coroutine.status(shellThread) ~= "dead" do
    deadline = os.clock() + 5
    local result
    if signal and signals[signal[1]] then
      xpcall(signals[signal[1]], onSignalError, select(2, table.unpack(signal)))
    else
      if not debug.gethook(shellThread) then
        debug.sethook(shellThread, timeoutHook, "", 10000)
      end
      local status = {coroutine.resume(shellThread)}
      if status[1] then
        -- All is well, in case we had a yield return the yielded value.
        if coroutine.status(shellThread) ~= "dead" then
          result = status[2]
        end
      elseif status[2].timeout then
        -- Timeout, restart the shell but don't start user scripts this time.
        startShell(true)
      else
        -- Some other error, go kill ourselves.
        error(result[2], 0)
      end
    end
    signal = {coroutine.yield(result)}
  end
end

-- JNLua (or possibly Lua itself) sucks at propagating error messages across
-- resumes that were triggered from the native side, so we do a pcall here.
local result, message = pcall(main)
--if not result then
  print(result, message)
--end
return result, message