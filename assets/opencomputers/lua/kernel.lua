--[[ This is called as the main coroutine by the host. If this returns the
     computer crashes. It should never ever return "normally", only when an
     error occurred. Shutdown / reboot are signalled via special yields. ]]

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

  getmetatable = getmetatable,
  setmetatable = setmetatable,

  _VERSION = "Lua 5.2",

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
    uptime = os.uptime,
    freeMemory = os.freeMemory,
    totalMemory = os.totalMemory,
    address = os.address,
    romAddress = os.romAddress,
    tmpAddress = os.tmpAddress
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
    upper = string.upper,
    uchar = string.uchar,
    ulen = string.ulen,
    ureverse = string.breverse,
    usub = string.bsub
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
sandbox._G = sandbox

function sandbox.load(code, source, env)
  return load(code, source, "t", env or sandbox)
end

function sandbox.checkArg(n, have, ...)
  have = type(have)
  local function check(want, ...)
    if not want then
      return false
    else
      return have == want or check(...)
    end
  end
  if not check(...) then
    local msg = string.format("bad argument #%d (%s expected, got %s)", n, table.concat({...}, " or "), have)
    --error(debug.traceback(msg, 2), 2)
    error(msg, 2)
  end
end

-------------------------------------------------------------------------------

--[[ Install wrappers for coroutine management that reserves the first value
     returned by yields for internal stuff. Used for sleeping and message
     calls (sendToAddress) that happen synchronized (Server thread).
--]]
local deadline = 0

local function checkDeadline()
  if os.realTime() > deadline then
    error("too long without yielding", 0)
  end
end

local function main(args)
  local function init()
    sandbox.driver.filesystem.mount(os.romAddress(), "/")
    sandbox.driver.filesystem.mount(os.tmpAddress(), "/tmp")
    local result, reason = sandbox.loadfile("/boot/init.lua")
    if not result then
      error(reason, 0)
    end
    return coroutine.create(result)
  end
  local co = init()
  while true do
    deadline = os.realTime() + timeout -- timeout global is set by host
    if not debug.gethook(co) then
      debug.sethook(co, checkDeadline, "", 10000)
    end
    local result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    if not result[1] then
      error(result[2] or "unknown error", 0)
    elseif coroutine.status(co) == "dead" then
      error("computer stopped unexpectedly", 0)
    else
      args = table.pack(coroutine.yield(result[2])) -- system yielded value
    end
  end
end

function sandbox.coroutine.resume(co, ...)
  local args = table.pack(...)
  while true do
    if not debug.gethook(co) then -- don't reset counter
      debug.sethook(co, checkDeadline, "", 10000)
    end
    local result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    checkDeadline()
    if result[1] then
      local isSystemYield = coroutine.status(co) ~= "dead" and result[2] ~= nil
      if isSystemYield then
        args = table.pack(coroutine.yield(result[2]))
      else
        return true, table.unpack(result, 3, result.n)
      end
    else -- error: result = (bool, string)
      return table.unpack(result, 1, result.n)
    end
  end
end

function sandbox.coroutine.yield(...)
  return coroutine.yield(nil, ...)
end

function sandbox.pcall(...)
  local result = table.pack(pcall(...))
  checkDeadline()
  return table.unpack(result, 1, result.n)
end

function sandbox.xpcall(...)
  local result = table.pack(xpcall(...))
  checkDeadline()
  return table.unpack(result, 1, result.n)
end

-------------------------------------------------------------------------------

function sandbox.os.shutdown()
  coroutine.yield(false)
end

function sandbox.os.reboot()
  coroutine.yield(true)
end

function sandbox.os.signal(name, timeout)
  local waitUntil = os.uptime() + (type(timeout) == "number" and timeout or math.huge)
  repeat
    local signal = table.pack(coroutine.yield(waitUntil - os.uptime()))
    if signal.n > 0 and (name == signal[1] or name == nil) then
      return table.unpack(signal, 1, signal.n)
    end
  until os.uptime() >= waitUntil
end

-------------------------------------------------------------------------------

sandbox.driver = {}

function sandbox.driver.componentType(id)
  return nodeName(id)
end

do
  local env = setmetatable({ send = sendToAddress },
                           { __index = sandbox, __newindex = sandbox })
  for name, code in pairs(drivers()) do
    local driver, reason = load(code, "=" .. name, "t", env)
    if not driver then
      print("Failed loading driver '" .. name .. "': " .. reason)
    else
      local result, reason = xpcall(driver, function(msg)
        return debug.traceback(msg, 2)
      end)
      if not result then
        print("Failed initializing driver '" .. name .. "': " ..
              (reason or "unknown error"))
      end
    end
  end
end

-------------------------------------------------------------------------------

-- JNLua converts the coroutine to a string immediately, so we can't get the
-- traceback later. Because of that we have to do the error handling here.
-- Also, yield once to allow initializing up to here to get a memory baseline.
return pcall(main, table.pack(coroutine.yield()))
