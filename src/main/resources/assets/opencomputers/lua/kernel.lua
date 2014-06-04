local hookInterval = 100
local deadline = math.huge
local hitDeadline = false
local function checkDeadline()
  if computer.realTime() > deadline then
    debug.sethook(coroutine.running(), checkDeadline, "", 1)
    if not hitDeadline then
      deadline = deadline + 0.5
    end
    hitDeadline = true
    error("too long without yielding", 0)
  end
end

-------------------------------------------------------------------------------

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
    local msg = string.format("bad argument #%d (%s expected, got %s)",
                              n, table.concat({...}, " or "), have)
    error(msg, 3)
  end
end

-------------------------------------------------------------------------------

local function spcall(...)
  local result = table.pack(pcall(...))
  if not result[1] then
    error(tostring(result[2]), 0)
  else
    return table.unpack(result, 2, result.n)
  end
end

--[[ This is the global environment we make available to userland programs. ]]
-- You'll notice that we do a lot of wrapping of native functions and adding
-- parameter checks in those wrappers. This is to avoid errors from the host
-- side that would push error objects - which are userdata and cannot be
-- persisted.
local sandbox, libprocess
sandbox = {
  assert = assert,
  dofile = nil, -- in boot/*_base.lua
  error = error,
  _G = nil, -- see below
  getmetatable = function(t)
    if type(t) == "string" then return nil end
    return getmetatable(t)
  end,
  ipairs = ipairs,
  load = function(ld, source, mode, env)
    if not system.allowBytecode() then
      mode = "t"
    end
    return load(ld, source, mode, env or sandbox)
  end,
  loadfile = nil, -- in boot/*_base.lua
  next = next,
  pairs = pairs,
  pcall = function(...)
    local result = table.pack(pcall(...))
    checkDeadline()
    return table.unpack(result, 1, result.n)
  end,
  print = nil, -- in boot/*_base.lua
  rawequal = rawequal,
  rawget = rawget,
  rawlen = rawlen,
  rawset = rawset,
  select = select,
  setmetatable = function(t, mt)
    local gc = rawget(mt, "__gc")
    if type(gc) == "function" then
      rawset(mt, "__gc", function(self)
        local co = coroutine.create(gc)
        debug.sethook(co, checkDeadline, "", hookInterval)
        local result, reason = coroutine.resume(co, self)
        debug.sethook(co)
        checkDeadline()
        if not result then
          error(reason, 0)
        end
      end)
    end
    local result = setmetatable(t, mt)
    rawset(mt, "__gc", gc)
    return result
  end,
  tonumber = tonumber,
  tostring = tostring,
  type = type,
  _VERSION = "Lua 5.2",
  xpcall = function(...)
    local result = table.pack(xpcall(...))
    checkDeadline()
    return table.unpack(result, 1, result.n)
  end,

  coroutine = {
    create = coroutine.create,
    resume = function(co, ...) -- custom resume part for bubbling sysyields
      checkArg(1, co, "thread")
      local args = table.pack(...)
      while true do -- for consecutive sysyields
        debug.sethook(co, checkDeadline, "", hookInterval)
        local result = table.pack(
          coroutine.resume(co, table.unpack(args, 1, args.n)))
        debug.sethook(co) -- avoid gc issues
        checkDeadline()
        if result[1] then -- success: (true, sysval?, ...?)
          if coroutine.status(co) == "dead" then -- return: (true, ...)
            return true, table.unpack(result, 2, result.n)
          elseif result[2] ~= nil then -- yield: (true, sysval)
            args = table.pack(coroutine.yield(result[2]))
          else -- yield: (true, nil, ...)
            return true, table.unpack(result, 3, result.n)
          end
        else -- error: result = (false, string)
          return false, result[2]
        end
      end
    end,
    running = coroutine.running,
    status = coroutine.status,
    wrap = function(f) -- for bubbling coroutine.resume
      local co = coroutine.create(f)
      return function(...)
        local result = table.pack(sandbox.coroutine.resume(co, ...))
        if result[1] then
          return table.unpack(result, 2, result.n)
        else
          error(result[2], 0)
        end
      end
    end,
    yield = function(...) -- custom yield part for bubbling sysyields
      return coroutine.yield(nil, ...)
    end
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
    random = function(...)
      return spcall(math.random, ...)
    end,
    randomseed = function(seed)
      spcall(math.randomseed, seed)
    end,
    sin = math.sin,
    sinh = math.sinh,
    sqrt = math.sqrt,
    tan = math.tan,
    tanh = math.tanh
  },

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

  io = nil, -- in lib/io.lua

  os = {
    clock = os.clock,
    date = function(format, time)
      return spcall(os.date, format, time)
    end,
    difftime = function(t2, t1)
      return t2 - t1
    end,
    execute = nil, -- in boot/*_os.lua
    exit = nil, -- in boot/*_os.lua
    remove = nil, -- in boot/*_os.lua
    rename = nil, -- in boot/*_os.lua
    time = function(table)
      checkArg(1, table, "table", "nil")
      return os.time(table)
    end,
    tmpname = nil, -- in boot/*_os.lua
  },

  debug = {
    traceback = debug.traceback
  },

  _OSVERSION = "OpenOS 1.2",
  checkArg = checkArg
}
sandbox._G = sandbox

-------------------------------------------------------------------------------
-- Start of non-standard stuff.

-- JNLua derps when the metatable of userdata is changed, so we have to
-- wrap and isolate it, to make sure it can't be touched by user code.
-- These functions provide the logic for wrapping and unwrapping (when
-- pushed to user code and when pushed back to the host, respectively).
local wrapUserdata, wrapSingleUserdata, unwrapUserdata, wrappedUserdataMeta
--[[
wrappedUserdataMeta = {
  -- Weak keys, clean up once a proxy is no longer referenced anywhere.
  __mode="k",
  -- We need custom persist logic here to avoid ERIS trying to save the
  -- userdata referenced in this table directly. It will be repopulated
  -- in the load methods of the persisted userdata wrappers (see below).
  __persist = function()
    return function()
      -- When using special persistence we have to manually reassign the
      -- metatable of the persisted value.
      return setmetatable({}, wrappedUserdataMeta)
    end
  end
}
local wrappedUserdata = setmetatable({}, wrappedUserdataMeta)
]]
local function processResult(result)
  wrapUserdata(result) -- needed for metamethods.
  if not result[1] then -- error that should be re-thrown.
    error(result[2], 0)
  else -- success or already processed error.
    return table.unpack(result, 2, result.n)
  end
end

local function invoke(target, direct, ...)
  local result
  if direct then
    result = table.pack(target.invoke(...))
    if result.n == 0 then -- limit for direct calls reached
      result = nil
    end
  end
  if not result then
    local args = table.pack(...) -- for access in closure
    result = select(1, coroutine.yield(function()
      unwrapUserdata(args)
      local result = table.pack(target.invoke(table.unpack(args, 1, args.n)))
      wrapUserdata(result)
      return result
    end))
  end
  return processResult(result)
end
--[[
local function udinvoke(f, data, ...)
  local args = table.pack(...)
  unwrapUserdata(args)
  local result = table.pack(f(data, table.unpack(args)))
  return processResult(result)
end

-- Metatable for additional functionality on userdata.
local userdataWrapper = {
  __index = function(self, ...)
    return udinvoke(userdata.apply, wrappedUserdata[self], ...)
  end,
  __newindex = function(self, ...)
    return udinvoke(userdata.unapply, wrappedUserdata[self], ...)
  end,
  __call = function(self, ...)
    return udinvoke(userdata.call, wrappedUserdata[self], ...)
  end,
  __gc = function(self)
    local data = wrappedUserdata[self]
    wrappedUserdata[self] = nil
    userdata.dispose(data)
  end,
  -- This is the persistence protocol for userdata. Userdata is considered
  -- to be 'owned' by Lua, and is saved to an NBT tag. We also get the name
  -- of the actual class when saving, so we can create a new instance via
  -- reflection when loading again (and then immediately wrap it again).
  -- Collect wrapped callback methods.
  __persist = function(self)
    print("start saving userdata " .. tostring(wrappedUserdata[self]))
    local className, nbt = userdata.save(wrappedUserdata[self])
    print("done saving userdata")
    -- The returned closure is what actually gets persisted, including the
    -- upvalues, that being the classname and a byte array representing the
    -- nbt data of the userdata value.
    return function()
      return wrapSingleUserdata(userdata.load(className, nbt))
    end
  end,
  -- Do not allow changing the metatable to avoid the gc callback being
  -- unset, leading to potential resource leakage on the host side.
  __metatable = "userdata",
  __tostring = "userdata"
}

local userdataCallback = {
  __call = function(self, ...)
    local methods = spcall(userdata.methods, wrappedUserdata[self.proxy])
    for name, direct in pairs(methods) do
      if name == self.name then
        return invoke(userdata, direct, wrappedUserdata[self.proxy], name, ...)
      end
    end
    error("no such method", 1)
  end,
  __tostring = function(self)
    return userdata.doc(wrappedUserdata[self.proxy], self.name) or "function"
  end
}

function wrapSingleUserdata(data)
  -- Reuse proxies for lower memory consumption and more logical behavior
  -- without the need of metamethods like __eq, as well as proper reference
  -- behavior after saving and loading again.
  for k, v in pairs(wrappedUserdata) do
    -- We need a custom 'equals' check for userdata because metamethods on
    -- userdata introduced by JNLua tend to crash the game for some reason.
    if v == data then
      return k
    end
  end
  local proxy = {type = "userdata"}
  local methods = spcall(userdata.methods, data)
  for method in pairs(methods) do
    proxy[method] = setmetatable({name=method, proxy=proxy}, userdataCallback)
  end
  wrappedUserdata[proxy] = data
  return setmetatable(proxy, userdataWrapper)
end

function wrapUserdata(values)
  local processed = {}
  local function wrapRecursively(value)
    if type(value) == "table" then
      if not processed[value] then
        processed[value] = true
        for k, v in pairs(value) do
          value[k] = wrapRecursively(v)
        end
      end
    elseif type(value) == "userdata" then
      return wrapSingleUserdata(value)
    end
    return value
  end
  wrapRecursively(values)
end

function unwrapUserdata(values)
  local processed = {}
  local function unwrapRecursively(value)
    if wrappedUserdata[value] then
      return wrappedUserdata[value]
    end
    if type(value) == "table" then
      if not processed[value] then
        processed[value] = true
        for k, v in pairs(value) do
          value[k] = unwrapRecursively(v)
        end
      end
    end
    return value
  end
  unwrapRecursively(values)
end
]]
function wrapUserdata(...) return ... end
function unwrapUserdata(...) return ... end

-------------------------------------------------------------------------------

local libcomponent

local componentCallback = {
  __call = function(self, ...)
    return libcomponent.invoke(self.address, self.name, ...)
  end,
  __tostring = function(self)
    return libcomponent.doc(self.address, self.name) or "function"
  end
}

libcomponent = {
  doc = function(address, method)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    local result, reason = spcall(component.doc, address, method)
    if not result and reason then
      error(reason, 2)
    end
    return result
  end,
  invoke = function(address, method, ...)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    local methods, reason = spcall(component.methods, address)
    if not methods then
      return nil, reason
    end
    for name, direct in pairs(methods) do
      if name == method then
        return invoke(component, direct, address, method, ...)
      end
    end
    error("no such method", 1)
  end,
  list = function(filter)
    checkArg(1, filter, "string", "nil")
    local list = spcall(component.list, filter)
    local key = nil
    return function()
      key = next(list, key)
      if key then
        return key, list[key]
      end
    end
  end,
  proxy = function(address)
    checkArg(1, address, "string")
    local type, reason = spcall(component.type, address)
    if not type then
      return nil, reason
    end
    local proxy = {address = address, type = type}
    local methods, reason = spcall(component.methods, address)
    if not methods then
      return nil, reason
    end
    for method in pairs(methods) do
      proxy[method] = setmetatable({address=address,name=method}, componentCallback)
    end
    return proxy
  end,
  type = function(address)
    checkArg(1, address, "string")
    return component.type(address)
  end
}
sandbox.component = libcomponent

local libcomputer = {
  isRobot = computer.isRobot,
  address = computer.address,
  tmpAddress = computer.tmpAddress,
  freeMemory = computer.freeMemory,
  totalMemory = computer.totalMemory,
  uptime = computer.uptime,
  energy = computer.energy,
  maxEnergy = computer.maxEnergy,

  getBootAddress = computer.getBootAddress,
  setBootAddress = function(address)
    return spcall(computer.setBootAddress, address)
  end,

  users = computer.users,
  addUser = function(name)
    return spcall(computer.addUser, name)
  end,
  removeUser = function(name)
    return spcall(computer.removeUser, name)
  end,

  shutdown = function(reboot)
    coroutine.yield(reboot ~= nil and reboot ~= false)
  end,
  pushSignal = function(name, ...)
    return spcall(computer.pushSignal, name, ...)
  end,
  pullSignal = function(timeout)
    local deadline = computer.uptime() +
      (type(timeout) == "number" and timeout or math.huge)
    repeat
      local signal = table.pack(coroutine.yield(deadline - computer.uptime()))
      if signal.n > 0 then
        return table.unpack(signal, 1, signal.n)
      end
    until computer.uptime() >= deadline
  end,

  beep = function(...)
    libcomponent.invoke(computer.address(), "beep", ...)
  end
}
sandbox.computer = libcomputer

local libunicode = {
  char = function(...)
    return spcall(unicode.char, ...)
  end,
  len = function(s)
    return spcall(unicode.len, s)
  end,
  lower = function(s)
    return spcall(unicode.lower, s)
  end,
  reverse = function(s)
    return spcall(unicode.reverse, s)
  end,
  sub = function(s, i, j)
    if j then
      return spcall(unicode.sub, s, i, j)
    end
    return spcall(unicode.sub, s, i)
  end,
  upper = function(s)
    return spcall(unicode.upper, s)
  end
}
sandbox.unicode = libunicode

-------------------------------------------------------------------------------

local function bootstrap()
  function boot_invoke(address, method, ...)
    local result = table.pack(pcall(invoke, component, true, address, method, ...))
    if not result[1] then
      return nil, result[2]
    else
      return table.unpack(result, 2, result.n)
    end
  end
  do
    local screen = libcomponent.list("screen")()
    local gpu = libcomponent.list("gpu")()
    if gpu and screen then
      boot_invoke(gpu, "bind", screen)
    end
  end
  local function tryLoadFrom(address)
    local handle, reason = boot_invoke(address, "open", "/init.lua")
    if not handle then
      return nil, reason
    end
    local buffer = ""
    repeat
      local data, reason = boot_invoke(address, "read", handle, math.huge)
      if not data and reason then
        return nil, reason
      end
      buffer = buffer .. (data or "")
    until not data
    boot_invoke(address, "close", handle)
    return load(buffer, "=init", "t", sandbox)
  end
  local init, reason
  if computer.getBootAddress() then
    init, reason = tryLoadFrom(computer.getBootAddress())
  end
  if not init then
    computer.setBootAddress()
    for address in libcomponent.list("filesystem") do
      init, reason = tryLoadFrom(address)
      if init then
        computer.setBootAddress(address)
        break
      end
    end
  end
  if not init then
    error("no bootable medium found" .. (reason and (": " .. tostring(reason)) or ""))
  end

  return coroutine.create(init), {n=0}
end

-------------------------------------------------------------------------------

local function main()
  -- Yield once to get a memory baseline.
  coroutine.yield()

  -- After memory footprint to avoid init.lua bumping the baseline.
  local co, args = bootstrap()

  while true do
    deadline = computer.realTime() + system.timeout()
    hitDeadline = false
    debug.sethook(co, checkDeadline, "", hookInterval)
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