local cache = {}
local function cload(callback)
  local c = cache[callback]
  if not c then
    c = callback()
    cache[callback] = c
  end
  return c
end

return function(proxy)
  return
  {
    contents = {read=proxy.get, write=proxy.set},
    data = {read=proxy.getData, write=proxy.setData},
    checksum = {read=proxy.getChecksum,size=function() return 8 end},
    size = {cload(proxy.getSize)},
    dataSize = {cload(proxy.getDataSize)},
    label = {write=proxy.setLabel,proxy.getLabel()},
    makeReadonly = {write=proxy.makeReadonly}
  }
end
