local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: which <program>")
  return
end

for i = 1, #args do
  local result, reason = shell.getAlias(args[i])
  if result then
    result = args[i] .. ": aliased to " .. result
  else
    result, reason = shell.resolve(args[i], "lua")
  end
  if result then
    io.write(result .. "\n")
  else
    io.stderr:write(args[i] .. ": " .. reason .. "\n")
  end
end