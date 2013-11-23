local args = shell.parse(...)
if #args < 1 then
  print("Usage: unalias <name>")
  return
end

local result = shell.getAlias(args[1])
if not result then
  print("no such alias")
else
  shell.setAlias(args[1], nil)
  print("alias removed: " .. args[1] .. " -> " .. result)
end
