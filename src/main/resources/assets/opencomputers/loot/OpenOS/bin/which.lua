local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: which <program>\n")
  return 255
end

for i = 1, #args do
  local result, reason = shell.resolve(args[i], "lua")
  
  if not result then
    result = shell.getAlias(args[i])
    if result then
      result = args[i] .. ": aliased to " .. result
    end
  end

  if result then
    print(result)
  else
    io.stderr:write(args[i] .. ": " .. reason .. "\n")
    return 1
  end
end
