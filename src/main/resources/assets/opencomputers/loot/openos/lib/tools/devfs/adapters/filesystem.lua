local fs = require("filesystem")
local text = require("text")

return function(proxy)
  return
  {
    ["label"] =
    {
      read = function() return proxy.getLabel() or "" end,
      write= function(v) proxy.setLabel(text.trim(v)) end
    },
    ["isReadOnly"] = {proxy.isReadOnly()},
    ["spaceUsed"] = {proxy.spaceUsed()},
    ["spaceTotal"] = {proxy.spaceTotal()},
    ["mounts"] = {read = function()
      local mounts = {}
      for mproxy,mpath in fs.mounts() do
        if mproxy.address == proxy.address then
          table.insert(mounts, mpath)
        end
      end
      return table.concat(mounts, "\n")
    end}
  }
end
