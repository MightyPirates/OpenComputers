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
  print(i .. ") " .. candidates[i].address)
end

print("To select the device to install to, please enter a number between 1 and " .. #candidates .. ".")
print("Press 'q' to cancel the installation.")
local choice
repeat
  local _, address, char, code = event.pull("key")
  if component.isPrimary(address) then
    local value = unicode.char(char)
    if value == "q" then
      return
    end
    local number = tonumber(value)
    if number and number > 0 and number <= #candidates then
      choice = number
    else
    end
  end
until choice
choice = candidates[choice]

local cp = loadfile(shell.resolve("cp","lua"))

print("Installing OpenOS to device " .. choice.address)
local boot = "/mnt/" .. computer.getBootAddress():sub(1, 3) .. "/"
local mnt = "/mnt/" .. choice.address:sub(1, 3) .. "/"
local function install(what, path)
  print("Installing " .. what .. "...")
  local result, reason = pcall(cp,"-vf",boot .. path,mnt)
  if not result then
    error(reason, 0)
  end
end
install("programs", "bin")
install("boot scripts", "boot")
install("libraries", "lib")
install("additional files", "usr")
install("startup script", "init.lua")
computer.setBootAddress(choice.address)

print("All done! Rebooting from device now...")
computer.shutdown(true)
