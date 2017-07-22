local computer = require("computer")
local shell = require("shell")

local args = shell.parse(...)
if #args ~= 1 then
  io.write("Usage: userdel <name>\n")
  return 1
end

if not computer.removeUser(args[1]) then
  io.stderr:write("no such user\n")
  return 1
end
