local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 1 then
  io.write("Usage: label [-a] <fs> [<label>]\n")
  io.write(" -a  File system is specified via label or address instead of by path.")
  return
end

local proxy, reason
if options.a then
  proxy, reason = fs.proxy(args[1])
else
  proxy, reason = fs.get(args[1])
end
if not proxy then
  io.stderr:write(reason)
  return
end

if #args < 2 then
  io.stderr:write(proxy.getLabel() or "no label")
else
  local result, reason = proxy.setLabel(args[2])
  if not result then
    io.stderr:write(reason or "could not set label")
  end
end
