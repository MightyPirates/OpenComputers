local fs = require("filesystem")
local shell = require("shell")
local component = require("component")

----------------------------------------

local args = shell.parse(...)
if not args[1] then
  print("usege:\n.первый аргумент uuid(можно сокрашенный)")
  return
end

local address, err = component.get(args[1])
if address then
    for proxy, path in fs.mounts() do
        if proxy.address == address then
            shell.setWorkingDirectory(path)
            return
        end
    end
end
print("dist not found")