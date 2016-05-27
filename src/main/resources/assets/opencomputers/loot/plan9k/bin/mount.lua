local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args == 0 then
  for proxy, path in fs.mounts() do
    local label = proxy.getLabel() or proxy.address
    local mode = proxy.isReadOnly() and "ro" or "rw"
    io.write(string.format("%s on %s (%s)\n", label, path, mode))
  end
  return
end
if #args < 2 then
  io.write("Usage: mount [label|address path]\n")
  io.write("Note that the address may be abbreviated.")
  return
end

local proxy, reason = fs.proxy(args[1])
if not proxy then
  io.stderr:write(reason)
  return
end

local result, reason = fs.mount(table.unpack(args))
if not result then
  io.stderr:write(reason)
end
