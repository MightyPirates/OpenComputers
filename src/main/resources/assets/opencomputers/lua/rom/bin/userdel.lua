local computer = require("computer")
local shell = require("shell")

local args = shell.parse(...)
if #args < 1 then
  io.write("Usage: userdel <name>")
  return
end

if not computer.removeUser(args[1]) then
  io.stderr:write("no such user")
end