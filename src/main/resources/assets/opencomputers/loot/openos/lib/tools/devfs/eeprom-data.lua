local comp = require("component")
local devfs = ...

-- eeprom get/set has to delayed because comp.eeprom may not be available
local node = devfs.new_callback_proxy(function() return comp.eeprom.getData() end, function(...) comp.eeprom.setData(...) end)
function node.isAvailable()
  return comp.list("eeprom", true)()
end

return node
