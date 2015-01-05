local component = require("component")
local shell = require("shell")
local fs = require("filesystem")

local args, options = shell.parse(...)

if #args < 1 and not options.l then
  io.write("Usage: flash [-qlr] [<bios.lua>] [label]\n")
  io.write(" q: quiet mode, don't ask questions.\n")
  io.write(" l: print current contents of installed EEPROM.\n")
  io.write(" r: save the current contents of installed EEPROM to file.")
  return
end

local eeprom = component.eeprom

if options.l then
  io.write(eeprom.get())
  return
end

if options.r then
  fileName = shell.resolve(args[1])
  if not options.q then
    if fs.exists(fileName) then
      io.write("Are you want to overwrite " .. fileName .. "?\n")
      io.write("Type `y` to confirm.\n")
      repeat
        local response = io.read()
      until response and response:lower():sub(1, 1) == "y"
    end
    io.write("Reading EEPROM " .. eeprom.address .. ".\n" )
  end
  file = io.open(fileName, 'wb')
  local bios = eeprom.get()
  file:write(bios)
  file:close()
  if not options.q then io.write("All done!\nThe label is '" .. eeprom.getLabel() .. "'.\n") end
  return
end

local file = assert(io.open(args[1], "rb"))

if not options.q then
  io.write("Insert the EEPROM you would like to flash.\n")
  io.write("When ready to write, type `y` to confirm.\n")
  repeat
    local response = io.read()
  until response and response:lower():sub(1, 1) == "y"
  io.write("Beginning to flash EEPROM.\n")
end

if not options.q then
  io.write("Flashing EEPROM " .. eeprom.address .. ".\n")
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
if label and #label > 0 then
  eeprom.setLabel(label)
  if not options.q then
    io.write("Set label to '" .. eeprom.getLabel() .. "'.\n")
  end
end

if not options.q then
  io.write("All done! You can remove the EEPROM and re-insert the previous one now.\n")
end
