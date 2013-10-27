--[[ Low level boot logic. ]]

--[[ Permanent value tables.

     These tables must contain all java callbacks (i.e. C functions, since
     they are wrapped on the native side using a C function, of course).
     They are used when persisting/unpersisting the state so that the
     persistence library knows which values it doesn't have to serialize
     (since it cannot persist C functions).
--]]
local perms, uperms = {}, {}

--[[ Used by the Java side to persist the state when the world is saved. ]]
function persist(value)
  return eris.persist(perms, value)
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