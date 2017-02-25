require("filesystem").mount(
setmetatable({
  address = "f5501a9b-9c23-1e7a-4afe-4b65eed9b88a"
},
{
  __index=function(tbl,key)
    local result =
    ({
      getLabel = "devfs",
      spaceTotal = 0,
      spaceUsed = 0,
      isReadOnly = false,
    })[key]

    if result ~= nil then
      return function() return result end
    end
    local lib = require("devfs")
    lib.register(tbl)
    return lib.proxy[key]
  end
}), "/dev")

