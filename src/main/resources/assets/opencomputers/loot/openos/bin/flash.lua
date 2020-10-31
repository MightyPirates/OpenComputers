local component = require("component")
local shell = require("shell")
local fs = require("filesystem")

local args, options = shell.parse(...)

if #args < 1 and not options.l then
  io.write("Usage: flash [-qlr] [<bios.lua>] [label]\n")
  io.write(" q: quiet mode, don't ask questions.\n")
  io.write(" l: print current contents of installed EEPROM.\n")
  io.write(" r: save the current contents of installed EEPROM to file.\n")
  return
end

local function printRom()
  local eeprom = component.eeprom
  io.write(eeprom.get())
end

local function readRom()
  local eeprom = component.eeprom
  local fileName = shell.resolve(args[1])
  if not options.q then
    if fs.exists(fileName) then
      io.write("Are you sure you want to overwrite " .. fileName .. "?\n")
      io.write("Type `y` to confirm.\n")
      repeat
        local response = io.read()
      until response and response:lower():sub(1, 1) == "y"
    end
    io.write("Reading EEPROM " .. eeprom.address .. ".\n" )
  end
  local bios = eeprom.get()
  local file = assert(io.open(fileName, "wb"))
  file:write(bios)
  file:close()
  if not options.q then
    io.write("All done!\nThe label is '" .. eeprom.getLabel() .. "'.\n")
  end
end

local function writeRom()
  local file = assert(io.open(args[1], "rb"))
  local bios = file:read("*a")
  file:close()

  if not options.q then
    io.write("Insert the EEPROM you would like to flash.\n")
    io.write("When ready to write, type `y` to confirm.\n")
    repeat
      local response = io.read()
    until response and response:lower():sub(1, 1) == "y"
    io.write("Beginning to flash EEPROM.\n")
  end

  local eeprom = component.eeprom

  if not options.q then
    io.write("Flashing EEPROM " .. eeprom.address .. ".\n")
    io.write("Please do NOT power down or restart your computer during this operation!\n")
  end

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
end

if options.l then
  printRom()
elseif options.r then
  readRom()
else
  writeRom()
end
