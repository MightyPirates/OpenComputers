local comp = require("component")
local text = require("text")

return
{
  open = function(mode)
    if ({r=true, rb=true})[mode] then
      return text.internal.reader(comp.eeprom.getData())
    end
    return text.internal.writer(comp.eeprom.setData, ({a=true,ab=true})[mode] and comp.eeprom.getData())
  end,
  size = function()
    return string.len(comp.eeprom.getData())
  end
}
