local args = shell.parse(...)
if #args < 1 then
  print("Usage: unalias <name>")
  return
end

local result = shell.alias(args[1], nil)
if not result then
  print("no such alias")
else
  print("alias removed: " .. args[1] .. " -> " .. result)
end
