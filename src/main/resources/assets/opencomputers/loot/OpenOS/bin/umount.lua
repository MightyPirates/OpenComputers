local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)

if #args < 1 then
  io.write("Usage: umount [-a] <mount>\n")
  io.write(" -a  Remove any mounts by file system label or address instead of by path. Note that the address may be abbreviated.\n")
  return 1
end

local proxy, reason
if options.a then
  proxy, reason = fs.proxy(args[1])
  if proxy then
    proxy = proxy.address
  end
else
  local path = shell.resolve(args[1])
  proxy, reason = fs.get(path)
  if proxy then
    proxy = reason -- = path
    if proxy ~= path then
      io.stderr:write("not a mount point\n")
      return 1
    end
  end
end
if not proxy then
  io.stderr:write(tostring(reason)..'\n')
  return 1
end

if not fs.umount(proxy) then
  io.stderr:write("nothing to unmount here\n")
  return 1
end
