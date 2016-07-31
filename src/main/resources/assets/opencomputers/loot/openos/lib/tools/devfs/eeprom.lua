local comp = require("component")
local text = require("text")

return
{
  open = function(mode)
    if ({r=true, rb=true})[mode] then
      return text.internal.reader(comp.eeprom.get())
    end
    return text.internal.writer(comp.eeprom.set, ({a=true,ab=true})[mode] and comp.eeprom.get())
  end,
  size = function()
    return string.len(comp.eeprom.get())
  end
}
