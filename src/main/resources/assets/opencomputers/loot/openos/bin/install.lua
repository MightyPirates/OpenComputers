local computer = require("computer")
local shell = require("shell")

local options

do
  local basic, reason = loadfile(package.searchpath("tools/install_basics", package.path), "bt", _G)
  if not basic then
    io.stderr:write("failed to load install: " .. tostring(reason) .. "\n")
    return 1
  end
  options = basic(...)
end

if not options then return end

if computer.freeMemory() < 50000 then
  print("Low memory, collecting garbage")
  for i=1,20 do os.sleep(0) end
end

local cp, reason = loadfile(shell.resolve("cp", "lua"), "bt", _G)

local ec = cp(table.unpack(options.cp_args))
if ec ~= nil and ec ~= 0 then
  return ec
end

local write = io.write
write("Installation complete!\n")

if options.setlabel then
  pcall(options.target.dev.setLabel, options.label)
end

if options.setboot then
  local address = options.target.dev.address
  if computer.setBootAddress(address) then
    write("Boot address set to " .. address)
  end
end

if options.reboot then
  write("Reboot now? [Y/n] ")
  if ((io.read() or "n").."y"):match("^%s*[Yy]") then
    write("\nRebooting now!\n")
    computer.shutdown(true)
  end
end

write("Returning to shell.\n")
