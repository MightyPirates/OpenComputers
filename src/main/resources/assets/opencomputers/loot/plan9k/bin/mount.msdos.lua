local fs = require("filesystem")
local shell = require("shell")
local fat = require("msdosfs")

local args, options = shell.parse(...)
if #args == 0 then
  for proxy, path in fs.mounts() do
    local label = proxy.getLabel() or proxy.address
    local mode = proxy.isReadOnly() and "ro" or "rw"
    print(string.format("%s on %s (%s)", label, path, mode))
  end
  return
end
if #args < 2 then
  print("Usage: mount.msdos blockdevice path")
  --print("Note that the addresses may be abbreviated.")
  return
end

local file = args[1]

local proxy = fat.proxy(file, 12)

local result, reason = fs.mount(proxy, args[2])
if not result then
  print(reason)
end
