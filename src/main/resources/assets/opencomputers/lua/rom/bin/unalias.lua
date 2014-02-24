local shell = require("shell")

local args = shell.parse(...)
if #args < 1 then
  io.write("Usage: unalias <name>")
  return
end

local result = shell.getAlias(args[1])
if not result then
  io.stderr:write("no such alias")
else
  shell.setAlias(args[1], nil)
  io.write("alias removed: " .. args[1] .. " -> " .. result)
end