local fs = require("filesystem")
local shell = require("shell")
local pipes = require("pipes")

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
  print("Usage: mount label|address label|address path]")
  print("Note that the addresses may be abbreviated.")
  return
end

local readproxy, reason = fs.proxy(args[1])
if not readproxy then
  print(reason)
  return
end

local writeproxy, reason = fs.proxy(args[2])
if not writeproxy then
  print(reason)
  return
end

local proxy = pipes.cowProxy(readproxy, writeproxy)

local result, reason = fs.mount(proxy, args[3])
if not result then
  print(reason)
end
