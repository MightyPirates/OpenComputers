--[[ This is called as the main coroutine by the host. If this returns the
     computer crashes. It should never ever return "normally", only when an
     error occurred. Shutdown / reboot are signalled via special yields. ]]

local deadline = 0

local function checkDeadline()
  if os.realTime() > deadline then
    error("too long without yielding", 0)
  end
end

local function checkArg(n, have, ...)
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
    error(msg, 2)
  end
end

--[[ Set up the global environment we make available to userland programs. ]]
local sandbox
sandbox = {
  -- Top level values. The selection of kept methods rougly follows the list
  -- as available on the Lua wiki here: http://lua-users.org/wiki/SandBoxes
  assert = assert,
  error = error,
  load = function(ld, source, mode, env)
    assert((mode or "t") == "t", "unsupported mode")
    return load(ld, source, "t", env or sandbox)
  end,
  pcall = function(...)
    local result = table.pack(pcall(...))
    checkDeadline()
    return table.unpack(result, 1, result.n)
  end,
  xpcall = function(...)
    local result = table.pack(xpcall(...))
    checkDeadline()
    return table.unpack(result, 1, result.n)
  end,

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

  checkArg = checkArg,

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

  --[[ Install wrappers for coroutine management that reserves the first value
       returned by yields for internal stuff. Used for sleeping and message
       calls (sendToAddress) that happen synchronized (Server thread).
  --]]
  coroutine = {
    create = coroutine.create,
    running = coroutine.running,
    resume = function(co, ...)
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
    end,
    status = coroutine.status,
    yield = function(...)
      return coroutine.yield(nil, ...)
    end,
    wrap = function(f) -- for sandbox's coroutine.resume
      local co = sandbox.coroutine.create(f)
      return function(...)
        local result = table.pack(sandbox.coroutine.resume(co, ...))
        if result[1] then
          return table.unpack(result, 2, result.n)
        else
          error(result[2], 0)
        end
      end
    end
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
    randomseed = function(seed)
      checkArg(1, seed, "number")
      math.randomseed(seed)
    end,
    sin = math.sin,
    sinh = math.sinh,
    sqrt = math.sqrt,
    tan = math.tan,
    tanh = math.tanh
  },

  os = {
    clock = os.clock,
    date = os.date,
    difftime = function(t2, t1)
      return t2 - t1
    end,
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
    ureverse = string.ureverse,
    usub = string.usub,
    trim = function(s) -- from http://lua-users.org/wiki/StringTrim
      local from = s:match("^%s*()")
      return from > #s and "" or s:match(".*%S", from)
    end
  },

  table = {
    concat = table.concat,
    insert = table.insert,
    pack = table.pack,
    remove = table.remove,
    sort = table.sort,
    unpack = table.unpack
  },

  -- TODO evaluate whether this can be used to do evil things (TM)
  debug = {
    traceback = debug.traceback
  }
}
sandbox._G = sandbox

-------------------------------------------------------------------------------

function sandbox.os.shutdown(reboot)
  coroutine.yield(reboot ~= nil and reboot ~= false)
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

local component = {}

component.list = componentList
component.type = componentType

-- We use a custom "protocol" where we return a boolean first, indicating
-- whether we had an error that should be re-thrown.
function component.invokeAsync(...)
  local result = table.pack(componentInvoke(...))
  if not result[1] then
    -- We caught an error but performed some custom tostring logic.
    error(result[2], 2)
  else
    -- We should return the results as they are.
    return table.unpack(result, 2, result.n)
  end
end

-------------------------------------------------------------------------------

--[[ Wrap message sending callbacks to synchronize them to the server thread.

     We generate a wrapper that will yield a closure that will perform the
     actual call. The host will switch to messaging state and perform the
     actual in the server thread (by running the yielded function), meaning
     we don't have to worry about mutlithreading interaction with other
     components of Minecraft.
--]]
-- OK, I admit this is a little crazy... here goes:
local function synchronized(f)
  -- This is the function that replaces the original API function. It is
  -- called from userland when it wants something from a driver.
  return function(...)
    local args = table.pack(...)
    -- What it does, is that it yields a function. That function is called
    -- from the server thread, to ensure synchronicity with the world.
    local result = coroutine.yield(function()
      -- It runs the actual API function protected mode. We return this as
      -- a table because a) we need it like this on the outside anyway and
      -- b) only the first item in the global stack is persisted.
      local result = table.pack(pcall(f, table.unpack(args, 1, args.n)))
      if not result[1] then
        -- We apply tostring to error messages immediately because JNLua
        -- pushes the original Java exceptions which cannot be persisted.
        result[2] = tostring(result[2])
      end
      return result
    end)
    -- The next time our executor runs it pushes that result and calls
    -- resume, so we get it via the yield. Thus: result = pcall(f, ...)
    if result[1] then
      -- API call was successful, return the results.
      return table.unpack(result, 2, result.n)
    else
      -- API call failed, re-throw the error.
      error(result[2], 2)
    end
  end
end

component.methods = synchronized(componentMethods)
component.invoke = synchronized(component.invokeAsync)

-------------------------------------------------------------------------------

sandbox.component = {}

function sandbox.component.list(filter)
  checkArg(1, filter, "string", "nil")
  return pairs(component.list(filter))
end

function sandbox.component.type(address)
  checkArg(1, address, "string")
  return component.type(address)
end

function sandbox.component.invoke(address, method, ...)
  checkArg(1, address, "string")
  checkArg(2, method, "string")
  return component.invoke(address, method, ...)
end

function sandbox.component.proxy(address)
  checkArg(1, address, "string")
  local type, reason = component.type(address)
  if not type then
    return nil, reason
  end
  local proxy = {address = address, type = type}
  local methods = component.methods(address)
  if methods then
    for method, asynchronous in pairs(methods) do
      local invoke
      if asynchronous then
        invoke = component.invokeAsync
      else
        invoke = component.invoke
      end
      proxy[method] = function(...)
        return invoke(address, method, ...)
      end
    end
  end
  return proxy
end

-------------------------------------------------------------------------------

local function main()
  local args
  local function bootstrap()
    local function rom(method, ...)
      return component.invokeAsync(os.romAddress(), method, ...)
    end

    -- Custom dofile implementation since we don't have the baselib yet.
    local function dofile(file)
      local handle, reason = rom("open", file)
      if not handle then
        error(reason)
      end
      if handle then
        local buffer = ""
        repeat
          local data = rom("read", handle, math.huge)
          if data then
            buffer = buffer .. data
          end
        until not data
        rom("close", handle)
        local program, reason = load(buffer, "=" .. file, "t", sandbox)
        buffer = nil
        if program then
          local result = table.pack(pcall(program))
          if result[1] then
            return table.unpack(result, 2, result.n)
          else
            error("error initializing lib '" .. file .. "': " .. result[2])
          end
        else
          error("error loading lib '" .. file .. "': " .. reason)
        end
      end
    end

    local init = {}
    for _, api in ipairs(rom("list", "lib")) do
      local path = "lib/" .. api
      if not rom("isDirectory", path) then
        local install = dofile(path)
        if type(install) == "function" then
          table.insert(init, install)
        end
      end
    end

    for _, install in ipairs(init) do
      install()
    end
    init = nil

    -- Yield once to allow initializing up to here to get a memory baseline.
    args = table.pack(coroutine.yield(0))

    return coroutine.create(load([[
      fs.mount(os.romAddress(), "/")
      fs.mount(os.tmpAddress(), "/tmp")
      for c, t in component.list() do
        event.fire("component_added", c, t)
      end
      term.clear()
      while true do
        local result, reason = os.execute("/bin/sh -v")
        if not result then
          error(reason)
        end
      end]], "=init", "t", sandbox))
  end
  local co = bootstrap()
  while true do
    deadline = os.realTime() + timeout -- timeout global is set by host
    if not debug.gethook(co) then
      debug.sethook(co, checkDeadline, "", 10000)
    end
    local result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    if not result[1] then
      error(tostring(result[2]), 0)
    elseif coroutine.status(co) == "dead" then
      error("computer stopped unexpectedly", 0)
    else
      args = table.pack(coroutine.yield(result[2])) -- system yielded value
    end
  end
end

-- JNLua converts the coroutine to a string immediately, so we can't get the
-- traceback later. Because of that we have to do the error handling here.
return pcall(main)