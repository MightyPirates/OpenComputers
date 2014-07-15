local component = require("component")
local computer = require("computer")
local event = require("event")
local unicode = require("unicode")
local shell = require("shell")

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

print("Installing OpenOS to device " .. (choice.getLabel() or choice.address))
os.sleep(0.25)
local boot = computer.getBootAddress():sub(1, 3)
local mnt = choice.address:sub(1, 3)
local result, reason = os.execute("/bin/cp -vr /mnt/" .. boot .. "/* /mnt/" .. mnt .. "/")
if not result then
  error(reason, 0)
end
computer.setBootAddress(choice.address)

print("All done! Would you like to reboot now? [Y/n]")
local result = io.read()
if not result or result == "" or result:sub(1, 1):lower() == "y" then
  print("\nRebooting now!")
  computer.shutdown(true)
end
print("Returning to shell.")