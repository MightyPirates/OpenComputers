local component = require("component")
local computer = require("computer")
local filesystem = require("filesystem")
local unicode = require("unicode")
local shell = require("shell")
local term = require("term")
local kernel = require("pipes")

local args, options = shell.parse(...)

local fromAddress = options.from and component.get(options.from) or filesystem.get(os.getenv("_")).address
local candidates = {}
for address in component.list("filesystem") do
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
  result = term.read()
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

local name = options.name or "Plan9k"
io.write("Installing " .. name .." to device " .. (choice.getLabel() or choice.address) .. "\n")

local dest = findMount(choice.address) .. "/"
--We only install base system
local pid = os.spawn("/usr/bin/mpt.lua", "-v", "--offline", "--root="..dest, "--mirror=/", "-S", "plan9k" )
kernel.joinThread(pid)

if not options.nolabelset then pcall(choice.setLabel, name) end

if not options.noreboot then
  io.write("All done! Reboot now? [Y/n]\n")
  local result = term.read()
  if not result or result == "" or result:sub(1, 1):lower() == "y" then
    io.write("\nRebooting now!\n")
    computer.shutdown(true)
  end
end
io.write("Returning to shell.\n")
