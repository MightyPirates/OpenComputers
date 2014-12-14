local component = require("component")
local shell = require("shell")

local args, options = shell.parse(...)

if #args < 1 and not options.l then
  io.write("Usage: flash [-ql] [<bios.lua>] [label]\n")
  io.write(" q: quiet mode, don't ask questions.\n")
  io.write(" l: print current contents of installed EEPROM.")
  return
end

if options.l then
  io.write(component.eeprom.get())
  return
end

local file = assert(io.open(args[1], "rb"))

if not options.q then
  io.write("Insert the EEPROM you would like to flash.\n")
  io.write("When ready to write, type `y` to confirm.\n")
  repeat
    local response = io.read()
  until response and response:lower():sub(1, 1) == "y"
  io.write("Beginning to flash EEPROM.")
end

local eeprom = component.eeprom

if not options.q then
  io.write("Beginning to flash EEPROM " .. eeprom.address .. ".\n")
  io.write("Please do NOT power down or restart your computer during this operation!\n")
end

local bios = file:read("*a")
file:close()

eeprom.set(bios)

local label = args[2]
if not options.q and not label then
  io.write("Enter new label for this EEPROM. Leave input blank to leave the label unchanged.\n")
  label = io.read()
end
if label then
  eeprom.setLabel(label)
  if not options.q then
    io.write("Set label to '" .. eeprom.getLabel() .. "'.\n")
  end
end

if not options.q then
  io.write("All done! You can remove the EEPROM and re-insert the previous one now.\n")
end