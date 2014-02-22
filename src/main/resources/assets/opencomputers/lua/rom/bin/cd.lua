local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: cd <dirname>")
else
  local result, reason = shell.setWorkingDirectory(shell.resolve(args[1]))
  if not result then
    io.stderr:write(reason)
  end
end
