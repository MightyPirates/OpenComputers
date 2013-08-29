-- List of whitelisted globals, declared in advance because it's used by the
-- functions we declare in here, too, to avoid tampering.
local g

-- Until we get to ingame screens we log to Java's stdout. This is only used to
-- report internal failures during startup. Once we reach the kernel print is
-- replaced with a function that writes to the internal screen buffer.
do
  local System, iprs, ts = java.require("java.lang.System"), ipairs, tostring
  _G.print = function(...)
    for _, value in iprs({...}) do
      System.out:print(ts(value))
    end
    System.out:println()
  end
end

print("test")

--[[
  Install custom coroutine logic that forces coroutines to yield.

  We replace the core functions: create, resume and yield.
  - create is replaced with a function that periodically forces the created
    coroutine to yield a string with the value "timeout".
  - yield is replaced with a function that prepends any yielded values with a
    nil value. This is purely to allow differentiating voluntary (normal)
    yields from timeouts.
  - resume is replaced with a function that checks the first value returned
    from a yielding function. If we had a timeout we bubble upward, by also
    yielding with a timeout. Otherwise normal yield functionality applies.
]]
--[[
do
  -- Keep a backup of this function because it will be removed from our sandbox.
  local create, resume, yield, unpack, sethook =
    coroutine.create, coroutine.resume, coroutine.yield,
    unpack, debug.sethook
  -- This is the function we install as the hook.
  local function check()
    -- TODO check if there's a C stack frame? (i.e. we missed something)
    yield("timeout")
  end
  -- Install our coroutine factory replacement which takes care of forcing the
  -- created coroutines to yield once in a while. This is primarily used to to
  -- avoid coroutines from blocking completely.
  function _G.coroutine.create(f)
    local co = create(f)
    sethook(co, check, "", 100000)
    return co
  end
  -- Replace yield function used from now on to be able to distinguish between
  -- voluntary and forced yields.
  function _G.coroutine.yield(...)
    return yield(nil, ...)
  end
  -- Replace the resume function with one that automatically forwards timeouts.
  function _G.coroutine.resume(...)
    while true do
      local result = {resume(...)}
      if result[1] and result[2] == "timeout" then
        return yield("timeout")
      else
        return result[1], unpack(result, 3)
      end
    end
  end
end
]]

--[[ Set up the global environment we make available to userspace programs. ]]
g = {
  -- Top level values. The selection of kept methods rougly follows the list
  -- as available on the Lua wiki here: http://lua-users.org/wiki/SandBoxes
  -- Some entries have been kept although they are marked as unsafe on the
  -- wiki, due to how we set up our environment: we clear the globals table,
  -- so it does not matter if user-space functions gain access to the global
  -- environment. We pretty much give all user-space code full control to
  -- mess up the VM on the Lua side, we just want to make sure they can never
  -- reach out to the Java side in an unintended way.
  assert = assert,
  error = error,
  pcall = pcall,
  xpcall = xpcall,

  ipairs = ipairs,
  next = next,
  pairs = pairs,

  rawequal = rawequal,
  rawget = rawget,
  rawset = rawset,

  select = select,
  unpack = unpack,
  type = type,
  tonumber = tonumber,
  tostring = tostring,

  -- Loadstring is OK because it's OK that the loaded chunk is in the global
  -- environment as mentioned in the comment above.
  loadstring = loadstring,

  -- We don't care what users do with metatables. The only raised concern was
  -- about breaking an environment, and we don't care about that.
  getmetatable = getmetatable,
  setmetatable = setmetatable,

  -- Same goes for environment setters themselves. We do use local environments
  -- for loaded scripts, but that's more for convenience than for control.
  getfenv = getfenv,
  setfenv = setfenv,

  -- Custom print that actually writes to the screen buffer.
  print = print,

  coroutine = {
    create = coroutine.create,
    resume = coroutine.resume,
    running = coroutine.running,
    status = coroutine.status,
    wrap = coroutine.wrap,
    yield = coroutine.yield
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
    maxn = table.maxn,
    remove = table.remove,
    sort = table.sort,
    --[[
      Because I need this this every so often I decided to include it in the
      base API. This allows copying tables shallow or deep, to a new table or
      into an existing one. Usage:
        table.copy(t)            -- new table, shallow copy
        table.copy(t, true)      -- new table, deep copy
        table.copy(t1, t2)       -- copy t1 to t2, shallow copy
        table.copy(t1, t2, true) -- copy t1 to t2, deep copy
    ]]
    copy =
      function(from, to, deep)
        g.assert(g.type(from) == "table",
          "bad argument #1 (table expected, got " .. g.type(from) .. ")")
        deep = deep or (other and g.type(other) ~= "table")
        local copied, shallowcopy, deepcopy = {}
        function shallowcopy(from, to)
          for k, v in g.pairs(from) do
            to[k] = (deep and g.type(v) == "table") and deepcopy(v) or v
          end
          return to
        end
        function deepcopy(t)
          if copied[t] then return copied[t] end
          copied[t] = {}
          return shallowcopy(t, copied[t])
        end
        return shallowcopy(from, g.type(to) == "table" and to or {})
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
    log10 = math.log10,
    max = math.max,
    min = math.min,
    modf = math.modf,
    pi = math.pi,
    pow = math.pow,
    rad = math.rad,
    -- TODO Check if different Java LuaState's interfere via this. If so we
    --      may have to create a custom random instance to replace the built
    --      in random functionality of Lua.
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
    -- TODO Evaluate whether this can actually get dangerous.
    traceback = debug.traceback,
  },

  debug = debug
}

_G.table.copy = g.table.copy
do return end

-- Clear the globals table (except for its self-reference) and copy sandbox.
local copy = g.table.copy
for k, _ in pairs(_G) do
  if k ~= "_G" then _G[k] = nil end
end
copy(g, _G)