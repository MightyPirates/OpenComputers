local shell = require("shell")

local args = shell.parse(...)

if #args == 0 then
  for name, value in shell.aliases() do
    io.write(name .. " " .. value .. "\n")
  end
elseif #args == 1 then
  local value = shell.getAlias(args[1])
  if value then
    io.write(value)
  else
    io.stderr:write("no such alias")
  end
else
  shell.setAlias(args[1], args[2])
  io.write("alias created: " .. args[1] .. " -> " .. args[2])
end