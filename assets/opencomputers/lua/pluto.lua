--[[ Build Pluto's permanent value tables. ]]
local perms, uperms = {[_G] = "_G"}, {["_G"] = _G}

-- Flattens nested tables to concatenate field names with points. This is done
-- to ensure we don't have any duplicates and to get the perm "names".
local function store(t)
  if not t then return end
  local function flattenAndStore(k, v)
    if type(v) == "function" then
      assert(uperms[k] == nil, "duplicate permanent value named " .. k)
      -- If we have aliases its enough to store the value once.
      if not perms[v] then
        perms[v] = k
        uperms[k] = v
      end
    elseif type(v) == "table" then
      local prefix = k .. "."
      for k, v in pairs(v) do
        flattenAndStore(prefix .. k, v)
      end
    end
  end
  for k, v in pairs(t) do
    flattenAndStore(k, v)
  end
end
store(_G)
store(...)

local print = ({...})[2]
if print then
  for k, v in pairs(uperms) do
    --print(k .. ":" .. type(v))
  end
end

return perms, uperms