local component = require("component")
local computer = require("computer")
local event = require("event")
local filesystem = require("filesystem")
local unicode = require("unicode")
local shell = require("shell")

local args, options = shell.parse(...)

local fromAddress = options.from and component.get(options.from) or filesystem.get(os.getenv("_")).address
local candidates = {}
for address in component.list("filesystem", true) do
  local dev = component.proxy(address)
  if not dev.isReadOnly() and dev.address ~= computer.tmpAddress() and dev.address ~= fromAddress then
    table.insert(candidates, dev)
  end
end

if #candidates == 0 then
  io.write("No writable disks found, aborting.\n")
  os.exit()
end

for i = 1, #candidates do
  local label = candidates[i].getLabel()
  if label then
    label = label .. " (" .. candidates[i].address:sub(1, 8) .. "...)"
  else
    label = candidates[i].address
  end
  io.write(i .. ") " .. label .. "\n")
end

io.write("To select the device to install to, please enter a number between 1 and " .. #candidates .. ".\n")
io.write("Press 'q' to cancel the installation.\n")
local choice
while not choice do
  result = io.read()
  if result:sub(1, 1):lower() == "q" then
    os.exit()
  end
  local number = tonumber(result)
  if number and number > 0 and number <= #candidates then
    choice = candidates[number]
  else
    io.write("Invalid input, please try again.\n")
  end
end

local function findMount(address)
  for fs, path in filesystem.mounts() do
    if fs.address == component.get(address) then
      return path
    end
  end
end

local name = options.name or "OpenOS"
io.write("Installing " .. name .." to device " .. (choice.getLabel() or choice.address) .. "\n")
os.sleep(0.25)
local cpPath = filesystem.concat(findMount(filesystem.get(os.getenv("_")).address), "bin/cp")
local cpOptions = "-vrx" .. (options.u and "ui " or "")
local cpSource = filesystem.concat(findMount(fromAddress), options.fromDir or "/", "*")
local cpDest = findMount(choice.address) .. "/"
local result, reason = os.execute(cpPath .. " " .. cpOptions .. " " .. cpSource .. " " .. cpDest)
if not result then
  error(reason, 0)
end
if not options.nolabelset then pcall(choice.setLabel, name) end

if not options.noreboot then
  io.write("All done! " .. ((not options.noboot) and "Set as boot device and r" or "R") .. "eboot now? [Y/n]\n")
  local result = io.read()
  if not result or result == "" or result:sub(1, 1):lower() == "y" then
    if not options.noboot then computer.setBootAddress(choice.address)end
    io.write("\nRebooting now!\n")
    computer.shutdown(true)
  end
end
io.write("Returning to shell.\n")
