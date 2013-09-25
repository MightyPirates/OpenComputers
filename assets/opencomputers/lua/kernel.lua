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
  if os.realTime() > deadline then
    error({timeout = debug.traceback(2)}, 0)
  end
end

--[[ Set up the global environment we make available to userland programs. ]]
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
  -- about breaking an environment, which is pretty much void in Lua 5.2.
  getmetatable = getmetatable,
  setmetatable = setmetatable,

  write = function() end,

  checkArg = checkArg,
  component = component,
  driver = driver,

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
    unpack = table.unpack
  }
}
function sandbox.print(...)
  sandbox.write(...)
  sandbox.write("\n")
end

-- Make the sandbox its own globals table.
sandbox._G = sandbox

-- Allow sandboxes to load code, but only in text form, and in the sandbox.
-- Note that we allow passing a custom environment, because if this is called
-- from inside the sandbox, env must already be in the sandbox.
function sandbox.load(code, env)
  return load(code, code, "t", env or sandbox)
end

--[[ Install wrappers for coroutine management that reserves the first value
     returned by yields for internal stuff. For now this is used for driver
     calls, in which case the first function is the function performing the
     actual driver call, but is called from the server thread, and for sleep
     notifications, i.e. letting the host know we're in no hurry to be
     called again any time soon. See os.sleep for more on the second.
--]]
function sandbox.coroutine.yield(...)
  return coroutine.yield(nil, ...)
end
function sandbox.coroutine.resume(co, ...)
  local function checkDeadline()
    if os.realTime() > deadline then
      error("too long without yielding", 0)
    end
  end
  local args = {...}
  while true do
    -- Don't reset counter when resuming, to avoid circumventing the timeout.
    if not debug.gethook(co) then
      debug.sethook(co, checkDeadline, "", 10000)
    end

    -- Run the coroutine.
    local result = {coroutine.resume(co, table.unpack(args))}

    -- Check if we're over the deadline since I'm pretty sure the counter of
    -- coroutines is separate.
    checkDeadline()

    -- See what kind of yield we have.
    if result[1] then
      -- Coroutine returned normally, if we yielded it may be a system yield.
      if coroutine.status(co) ~= "dead" and result[2] then
        -- Propagate system yields and repeat the retry.
        args = coroutine.yield(table.unpack(result, 2))
      else
        -- Normal yield or coroutine returned, return result.
        return result[1], table.unpack(result, 3)
      end
    else
      -- Error while executing coroutine.
      return table.unpack(result)
    end
  end
end

--[[ Pull a signal with an optional timeout. ]]
function sandbox.os.signal(name, timeout)
  checkArg(1, name, "string", "nil")
  checkArg(2, timeout, "number", "nil")
  local deadline = os.clock() + (timeout or math.huge)
  while os.clock() < deadline do
    local signal = {coroutine.yield(deadline - os.clock())}
    if signal and (signal[1] == name or name == nil) then
      return table.unpack(signal)
    end
  end
end

-- JNLua converts the coroutine to a string immediately, so we can't get the
-- traceback later. Because of that we have to do the error handling here.
return xpcall(function()
  -- Replace init script code with loaded, sandboxed and threaded script.
  local init = (function()
    local result, reason = load(init(), "init", "t", sandbox)
    if not result then error(reason, 0) end
    return coroutine.create(result)
  end)()

  -- Main kernel loop.
  local data = {}
  while true do
    deadline = os.realTime() + 3
    local result = {coroutine.resume(init, table.unpack(data))}
    if result[1] then
      -- Init should never return, so we have a system yield.
      result = result[2]
    else
      -- Some other error, go kill ourselves.
      error(result[2])
    end
    data = {coroutine.yield(result)}
  end
end, function(msg) return msg end)