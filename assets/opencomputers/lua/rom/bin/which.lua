local args = shell.parse(...)
if #args == 0 then
  print("Usage: which <program>")
  return
end

local result, reason = shell.resolve(args[1], "lua")
if result then
  print(result)
else
  print(reason)
end
