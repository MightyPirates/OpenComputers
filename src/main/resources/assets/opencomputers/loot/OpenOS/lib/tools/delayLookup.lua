local data,tbl,key = ...
local z = data[tbl]

if key then -- index
  local method = z.methods[key]
  local cache = z.cache[key]
  if method and not cache then
    local file = io.open(z.path,"r")
    if file then
      file:seek("set", method[1])
      local loaded = load("return function"..file:read(method[2]), "=delayed-"..key,"t",z.env)
      file:close()
      assert(loaded,"failed to load "..key)
      cache = loaded()
      --lazy_protect(key, cache)
      z.cache[key] = cache
    end
  end
  return cache
else -- pairs
  local set,k,v = {}
  while true do
    k,v = next(tbl,k)
    if not k then break end
    set[k] = v
  end
  for k in pairs(z.methods) do
    if not set[k] then
      set[k] = function(...)return tbl[k](...)end
    end
  end
  return pairs(set)
end
