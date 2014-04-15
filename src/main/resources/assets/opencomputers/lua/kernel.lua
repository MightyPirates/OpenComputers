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

local function invoke(direct, ...)
  local result
  if direct then
    result = table.pack(component.invoke(...))
    if result.n == 0 then -- limit for direct calls reached
      result = nil
    end
  end
  if not result then
    local args = table.pack(...) -- for access in closure
    result = select(1, coroutine.yield(function()
      return table.pack(component.invoke(table.unpack(args, 1, args.n)))
    end))
  end
  if not result[1] then -- error that should be re-thrown.
    error(result[2], 0)
  else -- success or already processed error.
    return table.unpack(result, 2, result.n)
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

local running = setmetatable({}, {__mode="k"})

local function findProcess(co)
  co = co or coroutine.running()
  for _, process in pairs(running) do
    for _, instance in pairs(process.instances) do
      if instance == co then
        return process
      end
    end
  end
end

-------------------------------------------------------------------------------

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
    if not allowBytecode() then
      mode = "t"
    end
    env = env or select(2, libprocess.running())
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
  setmetatable = setmetatable,
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
    create = function(f)
      local co = coroutine.create(f)
      table.insert(findProcess().instances, co)
      return co
    end,
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
    random = function(low, high)
      if low then
        checkArg(1, low, "number")
        if high then
          checkArg(1, high, "number")
          return math.random(low, high)
        end
        return math.random(low)
      end
      return math.random()
    end,
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
      checkArg(1, format, "string", "nil")
      checkArg(2, time, "number", "nil")
      return os.date(format, time)
    end,
    difftime = function(t2, t1)
      return t2 - t1
    end,
    execute = nil, -- in boot/*_os.lua
    exit = nil, -- in boot/*_os.lua
    remove = nil, -- in boot/*_os.lua
    rename = nil, -- in boot/*_os.lua
    time = os.time,
    tmpname = nil, -- in boot/*_os.lua
  },

  debug = {
    traceback = debug.traceback
  },

  _OSVERSION = "OpenOS 1.1",
  checkArg = checkArg
}
sandbox._G = sandbox

-------------------------------------------------------------------------------
-- Start of non-standard stuff.

local libcomponent

local callback = {
  __call = function(method, ...)
    return libcomponent.invoke(method.address, method.name, ...)
  end,
  __tostring = function(method)
    return libcomponent.doc(method.address, method.name) or "function"
  end
}

libcomponent = {
  doc = function(address, method)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    local result, reason = component.doc(address, method)
    if not result and reason then
      error(reason, 2)
    end
    return result
  end,
  invoke = function(address, method, ...)
    checkArg(1, address, "string")
    checkArg(2, method, "string")
    local methods, reason = component.methods(address)
    if not methods then
      return nil, reason
    end
    for name, direct in pairs(methods) do
      if name == method then
        return invoke(direct, address, method, ...)
      end
    end
    error("no such method", 1)
  end,
  list = function(filter)
    checkArg(1, filter, "string", "nil")
    local list = component.list(filter)
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
    local type, reason = component.type(address)
    if not type then
      return nil, reason
    end
    local proxy = {address = address, type = type}
    local methods, reason = component.methods(address)
    if not methods then
      return nil, reason
    end
    for method in pairs(methods) do
      proxy[method] = setmetatable({address=address,name=method}, callback)
    end
    return proxy
  end,
  type = function(address)
    checkArg(1, address, "string")
    return component.type(address)
  end
}

local libcomputer = {
  isRobot = computer.isRobot,
  address = computer.address,
  romAddress = computer.romAddress,
  tmpAddress = computer.tmpAddress,
  freeMemory = computer.freeMemory,
  totalMemory = computer.totalMemory,
  uptime = computer.uptime,
  energy = computer.energy,
  maxEnergy = computer.maxEnergy,

  users = computer.users,
  addUser = function(name)
    checkArg(1, name, "string")
    return computer.addUser(name)
  end,
  removeUser = function(name)
    checkArg(1, name, "string")
    return computer.removeUser(name)
  end,

  shutdown = function(reboot)
    coroutine.yield(reboot ~= nil and reboot ~= false)
  end,
  pushSignal = function(name, ...)
    checkArg(1, name, "string")
    local args = table.pack(...)
    for i = 1, args.n do
      checkArg(i + 1, args[i], "nil", "boolean", "string", "number")
    end
    return computer.pushSignal(name, ...)
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
  end
}

libprocess = {
  load = function(path, env, init, name)
    checkArg(1, path, "string")
    checkArg(2, env, "table", "nil")
    checkArg(3, init, "function", "nil")
    checkArg(4, name, "string", "nil")

    local process = findProcess()
    if process then
      env = env or process.env
    end
    env = setmetatable({}, {__index=env or sandbox})
    local code, reason = sandbox.loadfile(path, "t", env)
    if not code then
      return nil, reason
    end

    local thread = coroutine.create(function(...)
      if init then
        init()
      end
      return code(...)
    end)
    running[thread] = {
      path = path,
      command = name,
      env = env,
      parent = process,
      instances = setmetatable({thread}, {__mode="v"})
    }
    return thread
  end,
  running = function(level)
    level = level or 1
    local process = findProcess()
    while level > 1 and process do
      process = process.parent
      level = level - 1
    end
    if process then
      return process.path, process.env, process.command
    end
  end
}

local libunicode = {
  char = function(...)
    local args = table.pack(...)
    for i = 1, args.n do
      checkArg(i, args[i], "number")
    end
    return unicode.char(...)
  end,
  len = function(s)
    checkArg(1, s, "string")
    return unicode.len(s)
  end,
  lower = function(s)
    checkArg(1, s, "string")
    return unicode.lower(s)
  end,
  reverse = function(s)
    checkArg(1, s, "string")
    return unicode.reverse(s)
  end,
  sub = function(s, i, j)
    checkArg(1, s, "string")
    checkArg(2, i, "number")
    checkArg(3, j, "number", "nil")
    if j then
      return unicode.sub(s, i, j)
    end
    return unicode.sub(s, i)
  end,
  upper = function(s)
    checkArg(1, s, "string")
    return unicode.upper(s)
  end
}

-------------------------------------------------------------------------------

local function bootstrap()
  -- Minimalistic hard-coded pure async proxy for our ROM.
  local rom = {}
  function rom.invoke(method, ...)
    return invoke(true, computer.romAddress(), method, ...)
  end
  function rom.open(file) return rom.invoke("open", file) end
  function rom.read(handle) return rom.invoke("read", handle, math.huge) end
  function rom.close(handle) return rom.invoke("close", handle) end
  function rom.inits(file) return ipairs(rom.invoke("list", "boot")) end
  function rom.isDirectory(path) return rom.invoke("isDirectory", path) end

  -- Custom low-level loadfile/dofile implementation reading from our ROM.
  local function loadfile(file)
    local handle, reason = rom.open(file)
    if not handle then
      error(reason)
    end
    local buffer = ""
    repeat
      local data, reason = rom.read(handle)
      if not data and reason then
        error(reason)
      end
      buffer = buffer .. (data or "")
    until not data
    rom.close(handle)
    return load(buffer, "=" .. file, "t", sandbox)
  end
  local function dofile(file)
    local program, reason = loadfile(file)
    if program then
      local result = table.pack(pcall(program))
      if result[1] then
        return table.unpack(result, 2, result.n)
      else
        error(result[2])
      end
    else
      error(reason)
    end
  end

  -- Load file system related libraries we need to load other stuff moree
  -- comfortably. This is basically wrapper stuff for the file streams
  -- provided by the filesystem components.
  local package = dofile("/lib/package.lua")

  -- Initialize the package module with some of our own APIs.
  package.preload["buffer"] = loadfile("/lib/buffer.lua")
  package.preload["component"] = function() return libcomponent end
  package.preload["computer"] = function() return libcomputer end
  package.preload["filesystem"] = loadfile("/lib/filesystem.lua")
  package.preload["io"] = loadfile("/lib/io.lua")
  package.preload["process"] = function() return libprocess end
  package.preload["unicode"] = function() return libunicode end

  -- Inject the package and io modules into the global namespace, as in Lua.
  sandbox.package = package
  sandbox.io = sandbox.require("io")

  -- Mount the ROM and temporary file systems to allow working on the file
  -- system module from this point on.
  sandbox.require("filesystem").mount(computer.romAddress(), "/")
  if computer.tmpAddress() then
    sandbox.require("filesystem").mount(computer.tmpAddress(), "/tmp")
  end

  -- Run library startup scripts. These mostly initialize event handlers.
  local scripts = {}
  for _, file in rom.inits() do
    local path = "boot/" .. file
    if not rom.isDirectory(path) then
      table.insert(scripts, path)
    end
  end
  table.sort(scripts)
  for i = 1, #scripts do
    dofile(scripts[i])
  end

  return coroutine.create(function() dofile("/init.lua") end), {n=0}
end

local function main()
  -- Make all calls in the bootstrapper direct to speed up booting.
  local realInvoke = invoke
  invoke = function(_, ...) return realInvoke(true, ...) end

  local co, args = bootstrap()

  -- Step out of the fast lane, all the basic stuff should now be loaded.
  invoke = realInvoke

  -- Yield once to get a memory baseline.
  coroutine.yield()

  while true do
    deadline = computer.realTime() + timeout -- timeout global is set by host
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
