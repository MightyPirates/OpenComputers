local deadline = 0
local realTime = os.realTime
local function checkDeadline()
  if realTime() > deadline then
    error("too long without yielding", 0)
  end
end

-------------------------------------------------------------------------------

local function invoke(asynchronous, ...)
  local result
  if asynchronous then
    result = table.pack(component.invoke(...))
  else
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

--[[ This is the global environment we make available to userland programs. ]]
-- You'll notice that we do a lot of wrapping of native functions and adding
-- parameter checks in those wrappers. This is to avoid errors from the host
-- side that would push error objects - which are userdata and cannot be
-- persisted.
local sandbox
sandbox = {
  assert = assert,
  -- dofile is reimplemented in lib/base.lua
  error = error,
  -- _G is set below
  getmetatable = getmetatable,
  ipairs = ipairs,
  load = function(ld, source, mode, env)
    assert((mode or "t") == "t", "unsupported mode")
    return load(ld, source, "t", env or sandbox)
  end,
  -- loadfile is reimplemented in lib/base.lua
  next = next,
  pairs = pairs,
  pcall = function(...)
    local result = table.pack(pcall(...))
    checkDeadline()
    return table.unpack(result, 1, result.n)
  end,
  -- print is reimplemented in lib/base.lua
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
    create = coroutine.create,
    resume = function(co, ...) -- custom resume part for bubbling sysyields
      if co == coroutine.running() then
        return nil, "cannot resume non-suspended coroutine"
      end
      local args = table.pack(...)
      while true do -- for consecutive sysyields
        debug.sethook(co, checkDeadline, "", 10000)
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
    upper = string.upper,
    uchar = string.uchar,

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

  -- io library is largely reimplemented in lib/io.lua

  os = {
    clock = os.clock,
    date = os.date,
    difftime = function(t2, t1)
      return t2 - t1
    end,
    -- execute is reimplemented in lib/os.lua
    -- exit is reimplemented in lib/os.lua
    -- remove is reimplemented in lib/os.lua
    -- rename is reimplemented in lib/os.lua
    time = os.time,
    -- tmpname is reimplemented in lib/os.lua

-------------------------------------------------------------------------------
-- Start of non-standard stuff.

    address = os.address,
    freeMemory = os.freeMemory,
    totalMemory = os.totalMemory,
    uptime = os.uptime,

    shutdown = function(reboot)
      coroutine.yield(reboot ~= nil and reboot ~= false)
    end,
    pushSignal = function(name, ...)
      checkArg(1, name, "string")
      local args = table.pack(...)
      for i = 1, args.n do
        checkArg(i + 1, args[i], "nil", "boolean", "string", "number")
      end
      return os.pushSignal(name, ...)
    end,
    pullSignal = function(timeout)
      local deadline = os.uptime() +
        (type(timeout) == "number" and timeout or math.huge)
      repeat
        local signal = table.pack(coroutine.yield(deadline - os.uptime()))
        if signal.n > 0 then -- not a "blind" resume?
          return table.unpack(signal, 1, signal.n)
        end
      until os.uptime() >= deadline
    end
  },

  component = {
    invoke = function(address, method, ...)
      checkArg(1, address, "string")
      checkArg(2, method, "string")
      return invoke(false, address, method, ...)
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
      local methods = component.methods(address)
      if methods then
        for method, asynchronous in pairs(methods) do
          proxy[method] = function(...)
            return invoke(asynchronous, address, method, ...)
          end
        end
      end
      return proxy
    end,
    type = function(address)
      checkArg(1, address, "string")
      return component.type(address)
    end
  },

  unicode = {
    char = unicode.char,
    len = unicode.len,
    lower = string.lower,
    reverse = unicode.reverse,
    sub = unicode.sub,
    upper = string.upper
  },

  checkArg = checkArg
}
sandbox._G = sandbox

-------------------------------------------------------------------------------

local function main()
  local args
  local function bootstrap()
    do
      -- Minimalistic hard-coded proxy for our ROM.
      local rom = {}
      function rom.invoke(method, ...)
        return invoke(true, os.romAddress(), method, ...)
      end
      function rom.open(file) return rom.invoke("open", file) end
      function rom.read(handle) return rom.invoke("read", handle, math.huge) end
      function rom.close(handle) return rom.invoke("close", handle) end
      function rom.libs(file) return ipairs(rom.invoke("list", "lib")) end
      function rom.isDirectory(path) return rom.invoke("isDirectory", path) end

      -- Custom dofile implementation since we don't have the baselib yet.
      local function dofile(file)
        local handle, reason = rom.open(file)
        if not handle then
          error(reason)
        end
        if handle then
          local buffer = ""
          repeat
            local data = rom.read(handle)
            if data then
              buffer = buffer .. data
            end
          until not data
          rom.close(handle)
          local program, reason = load(buffer, "=" .. file, "t", sandbox)
          if program then
            local result = table.pack(pcall(program))
            if result[1] then
              return table.unpack(result, 2, result.n)
            else
              error("error initializing lib: " .. result[2])
            end
          else
            error("error loading lib: " .. reason)
          end
        end
      end

      local init = {}
      for _, lib in rom.libs() do
        local path = "lib/" .. lib
        if not rom.isDirectory(path) then
          local install = dofile(path)
          if type(install) == "function" then
            table.insert(init, install)
          end
        end
      end

      for _, install in ipairs(init) do
        install()
      end
    end

    -- Yield once to get a memory baseline.
    args = table.pack(coroutine.yield(0)) -- pseudo sleep to avoid dying

    return coroutine.create(load(string.format([[
      fs.mount("%s", "/")
      fs.mount("%s", "/tmp")
      for c, t in component.list() do
        os.pushSignal("component_added", c, t)
      end
      term.clear()
      while true do
        local result, reason = os.execute("/bin/sh -v")
        if not result then
          print(reason)
        end
      end]], os.romAddress(), os.tmpAddress()), "=init", "t", sandbox))
  end
  local co = bootstrap()
  while true do
    deadline = os.realTime() + timeout -- timeout global is set by host
    debug.sethook(co, checkDeadline, "", 10000)
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