--[[ Low level boot logic. ]]

--[[ Argument checking for functions. ]]
function checkArg(n, have, ...)
  have = type(have)
  for _, want in pairs({...}) do
    if have == want then return end
  end
  error("bad argument #" .. n .. " (" .. table.concat({...}, " or ") ..
        " expected, got " .. have .. ")", 3)
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
        local result = {pcall(f, table.unpack(args))}
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
        return select(2, table.unpack(result))
      else
        -- API call failed, re-throw the error.
        error(result[2], 2)
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
  wrapRecursive(driver)
end