local component = require("component")
local computer = require("computer")
local event = require("event")
local unicode = require("unicode")
local shell = require("shell")

local args, options = shell.parse(...)

local candidates = {}
for address in component.list("filesystem") do
  local dev = component.proxy(address)
  if not dev.isReadOnly() and dev.address ~= computer.tmpAddress() then
    table.insert(candidates, dev)
  end
end

if #candidates == 0 then
  print("No writable disks found, aborting.")
  return
end

for i = 1, #candidates do
  local label = candidates[i].getLabel()
  if label then
    label = label .. " (" .. candidates[i].address:sub(1, 8) .. "...)"
  else
    label = candidates[i].address
  end
  print(i .. ") " .. label)
end

print("To select the device to install to, please enter a number between 1 and " .. #candidates .. ".")
print("Press 'q' to cancel the installation.")
local choice
while not choice do
  result = io.read()
  if result:sub(1, 1):lower() == "q" then
    return
  end
  local number = tonumber(result)
  if number and number > 0 and number <= #candidates then
    choice = candidates[number]
  else
    print("Invalid input, please try again.")
  end
end
candidates = nil

local name = options.name or "OpenOS"
print("Installing " .. name .." to device " .. (choice.getLabel() or choice.address))
os.sleep(0.25)
local origin = options.from and options.from:sub(1,3) or computer.getBootAddress():sub(1, 3)
local fromDir = options.fromDir or "/"
local mnt = choice.address:sub(1, 3)
local result, reason = os.execute("/bin/cp -vr /mnt/" .. origin .. fromDir .. "* /mnt/" .. mnt .. "/")
if not result then
  error(reason, 0)
end
if not options.nolabelset then pcall(choice.setLabel, name) end

if not options.noreboot then
  print("All done! " .. ((not options.noboot) and "Set as boot device and r" or "R") .. "eboot now? [Y/n]")
  local result = io.read()
  if not result or result == "" or result:sub(1, 1):lower() == "y" then
    if not options.noboot then computer.setBootAddress(choice.address)end
    print("\nRebooting now!")
    computer.shutdown(true)
  end
end
print("Returning to shell.")
