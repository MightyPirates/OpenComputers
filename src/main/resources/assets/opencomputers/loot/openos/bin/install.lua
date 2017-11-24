local computer = require("computer")
local shell = require("shell")

local options

do
  local basic, reason = loadfile("/lib/core/install_basics.lua", "bt", _G)
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
assert(cp, reason)

local ok, ec = pcall(cp, table.unpack(options.cp_args))
assert(ok, ec)

if ec ~= nil and ec ~= 0 then
  return ec
end

print("Installation complete!")

if options.setlabel then
  pcall(options.target.dev.setLabel, options.label)
end

if options.setboot then
  local address = options.target.dev.address
  if computer.setBootAddress(address) then
    print("Boot address set to " .. address)
  end
end

if options.reboot then
  io.write("Reboot now? [Y/n] ")
  if ((io.read() or "n").."y"):match("^%s*[Yy]") then
    print("\nRebooting now!\n")
    computer.shutdown(true)
  end
end

print("Returning to shell.\n")
