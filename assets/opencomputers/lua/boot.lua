--[[ Low level boot logic. ]]

--[[ Argument checking for functions. ]]
function checkType(n, have, ...)
  have = type(have)
  for _, want in pairs({...}) do
    if have == want then return end
  end
  error("bad argument #" .. n .. " (" .. table.concat({...}, " or ") ..
        " expected, got " .. have .. ")", 3)
end

--[[ The following code is used to allow making tables read-only. It is, by and
     large, inspired by http://lua-users.org/wiki/RecursiveReadOnlyTables
     We override some library functions in the sandbox we create to enforce
     honoring the fact that tables are readonly, such as rawset.
--]]

-- Store metatables that have been made read-only. This allows us to uniquely
-- identify such tables, without users being able to fake it.
local roproxies = setmetatable({}, {__mode="kv"}) -- real table -> proxy
local rometatables = setmetatable({}, {__mode="k"}) -- proxy -> metatable

--[[ Create a read-only proxy of a table. ]]
function table.asreadonly(t)
  checkType(1, t, "table")
  local wrap
  local function roindex(t)
    return function(_, k)
      local value = t[k]
      if type(value) == "table" then
        value = wrap(value) -- wrap is cached
      end
      return value
    end
  end
  local function ronewindex(_, _, _)
    error("trying to modify read-only table", 2)
  end
  local function ropairs(t)
    local function ronext(_, k)
      local nk, nv = next(t, k)
      if type(nv) == "table" then
        nv = wrap(nv) -- wrap is cached
      end
      return nk, nv
    end
    return function(_)
      return ronext, nil, nil
    end
  end
  local function wrap(t)
    if not roproxies[t] then
      local metatable = { __index = roindex(t),
                          __newindex = ronewindex,
                          __pairs = ropairs(t),
                          __metatable = "read only table" }
      rometatables[proxy] = metatable
      roproxies[t] = setmetatable({}, metatable)
    end
    return roproxies[t]
  end
  return wrap(t)
end

--[[ Allow checking if a table is read-only. ]]
function table.isreadonly(t)
  return rometatables[t] ~= nil
end

--[[ Because I need this this every so often I decided to include it in the
     base API. This allows copying tables shallow or deep, to a new table or
     into an existing one. Usage:
        table.copy(t)            -- new table, shallow copy
        table.copy(t, true)      -- new table, deep copy
        table.copy(t1, t2)       -- copy t1 to t2, shallow copy
        table.copy(t1, t2, true) -- copy t1 to t2, deep copy
--]]
function table.copy(from, to, deep)
  checkType(1, from, "table")
  checkType(2, to, "table", "boolean", "nil")
  checkType(3, deep, "boolean", "nil")

  deep = deep or (to and type(to) ~= "table")
  local copied, shallowcopy, deepcopy = {}
  function shallowcopy(from, to)
    for k, v in pairs(from) do
      to[k] = (deep and type(v) == "table") and deepcopy(v) or v
    end
    return to
  end
  function deepcopy(t)
    if copied[t] then return copied[t] end
    copied[t] = {}
    return shallowcopy(t, copied[t])
  end
  return shallowcopy(from, type(to) == "table" and to or {})
end

--[[ Wrap all driver callbacks.

     For each driver we generate a wrapper that will yield a closure that
     will perform the actual call. This way the actual call can be performed
     in the server thread, meaning we don't have to worry about mutlithreading
     interaction with other components of Minecraft.
--]]
do
  -- OK, I admit this is a little crazy... here goes:
  local function wrap(f)
    -- This is the function that replaces the original API function. It is
    -- called from userland when it wants something from a driver.
    return function(...)
      local args = {...}
      -- What it does, is that it yields a function. That function is called
      -- from the server thread, to ensure synchronicity with the world.
      local result = coroutine.yield(function()
        -- It runs the actual API function protected mode. We return this as
        -- a table because a) we need it like this on the outside anyway and
        -- b) only the first item in the global stack is persisted.
        return {pcall(f, table.unpack(args))}
      end)
      -- The next time our executor runs it pushes that result and calls
      -- resume, so we get it via the yield. Thus: result = pcall(f, ...)
      if result[1] then
        -- API call was successful, return the results.
        return select(2, table.unpack(result))
      else
        -- API call failed, re-throw the error. We apply tostring to it
        -- because JNLua pushes the original Java exceptions.
        error(tostring(result[2]), 2)
      end
    end
  end

  -- There really shouldn't be any cycles in the API table, but to be safe...
  local done = {}
  local function wrapRecursive(t)
    if done[t] then return end
    done[t] = true
    for k, v in pairs(t) do
      if type(v) == "function" then
        t[k] = wrap(v)
      elseif type(v) == "table" then
        wrapRecursive(v)
      end
    end
  end
  wrapRecursive(drivers)
end

--[[ Permanent value tables.

     These tables must contain all java callbacks (i.e. C functions, since
     they are wrapped on the native side using a C function, of course).
     They are used when persisting/unpersisting the state so that the
     persistence library knows which values it doesn't have to serialize
     (since it cannot persist C functions).
     These tables may change after loading a game, for example due to a new
     mod being installed or an old one being removed. In that case, the
     persistence library will throw an error while unpersisting, leading
     to what will essentially be a computer crash; which is pretty much
     the best way to tackle this, I think.
--]]
do
  local perms, uperms = {}, {}

  --[[ Used by the Java side to persist the state when the world is saved. ]]
  function persist(kernel)
    return eris.persist(perms, kernel)
  end

  --[[ Used by the Java side unpersist the state when the world is loaded. ]]
  function unpersist(value)
    if value and type(value) == "string" and value:len() > 0 then
      return eris.unpersist(uperms, value)
    else
      return nil
    end
  end

  --[[ Flattens nested tables to concatenate field names with points. This is
       done to ensure we don't have any duplicates and to get the perm "names".
  --]]
  local function flattenAndStore(k, v)
    -- We only care for tables and functions, any value types are safe.
    if type(v) == "table" or type(v) == "function" then
      assert(uperms[k] == nil, "duplicate permanent value named " .. k)
      -- If we have aliases its enough to store the value once.
      if perms[v] then return end
      perms[v] = k
      uperms[k] = v
      -- Recurse into tables.
      if type(v) == "table" then
        -- Enforce a deterministic order when determining the keys, to ensure
        -- the keys are the same when unpersisting again.
        local keys = {}
        for ck, _ in pairs(v) do
          table.insert(keys, ck)
        end
        table.sort(keys)
        for _, ck in ipairs(keys) do
          flattenAndStore(k .. "." .. ck, v[ck])
        end
      end
    end
  end

  -- Mark everything that's globally reachable at this point as permanent.
  flattenAndStore("_ENV", _ENV)
end