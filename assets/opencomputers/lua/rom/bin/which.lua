local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: which <program>")
  return
end

local result, reason = shell.resolve(args[1], "lua")
if result then
  io.write(result)
else
  io.write(reason)
end