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
    totalMemory = os.totalMemory,
    address = os.address
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
sandbox._G = sandbox

-- Note: 'write' will be replaced by init script.
function sandbox.write(...) end
function sandbox.print(...)
  sandbox.write(...)
  sandbox.write("\n")
end

function sandbox.load(code, source, env)
  return load(code, source, "t", env or sandbox)
end

--[[ Install wrappers for coroutine management that reserves the first value
     returned by yields for internal stuff. Used for sleeping and message
     calls (sendToNode and its ilk) that happen synchronized (Server thread).
--]]
local deadline = 0

local function checkDeadline()
  if os.realTime() > deadline then
    error("too long without yielding", 0)
  end
end

local function main(co)
  local args = {}
  while true do
    deadline = os.realTime() + timeout -- timeout global is set by host
    if not debug.gethook(co) then
      debug.sethook(co, checkDeadline, "", 10000)
    end
    local result = {coroutine.resume(co, table.unpack(args))}
    if result[1] then
      args = {coroutine.yield(result[2])} -- system yielded value
    else
      error(result[2])
    end
  end
end

function sandbox.coroutine.resume(co, ...)
  local args = {...}
  while true do
    if not debug.gethook(co) then -- don't reset counter
      debug.sethook(co, checkDeadline, "", 10000)
    end
    local result = {coroutine.resume(co, table.unpack(args))}
    checkDeadline()
    if result[1] then
      local isSystemYield = coroutine.status(co) ~= "dead" and result[2] ~= nil
      if isSystemYield then
        args = coroutine.yield(result[2])
      else
        return true, table.unpack(result, 3)
      end
    else -- error: result = (bool, string)
      return table.unpack(result)
    end
  end
end

function sandbox.coroutine.yield(...)
  return coroutine.yield(nil, ...)
end

function sandbox.os.signal(name, timeout)
  local waitUntil = os.clock() + (type(timeout) == "number" and timeout or math.huge)
  while os.clock() < waitUntil do
    local signal = {coroutine.yield(waitUntil - os.clock())}
    if signal and (name == signal[1] or name == nil) then
      return table.unpack(signal)
    end
  end
end

function sandbox.os.shutdown()
  coroutine.yield(false)
end

function sandbox.os.reboot()
  coroutine.yield(true)
end

sandbox.driver = {}

function sandbox.driver.componentType(id)
  return nodeName(id)
end

do
  local env = setmetatable({
                sendToNode = sendToNode,
                sendToAll = sendToAll
              }, { __index = sandbox })
  for name, code in pairs(drivers()) do
    local driver, reason = load(code, "=" .. name, "t", env)
    if not driver then
      print("Failed loading driver '" .. name .. "': " .. reason)
    else
      local result, reason = pcall(driver)
      if not result then
        print("Failed initializing driver '" .. name .. "': " .. reason)
      end
    end
  end
end

-- Load init script in sandboxed environment.
local coinit
do
  local result, reason = load(init(), "=init", "t", sandbox)
  if not result then
    error(reason)
  end
  coinit = coroutine.create(result)
end

-- Yield once to allow initializing up to here to get a memory baseline.
coroutine.yield()

-- JNLua converts the coroutine to a string immediately, so we can't get the
-- traceback later. Because of that we have to do the error handling here.
return pcall(main, coinit)