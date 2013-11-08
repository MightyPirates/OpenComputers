local args = shell.parse(...)
if #args < 1 then
  print("Usage: useradd <name>")
  return
end

local result, reason = os.addUser(args[1])
if not result then
  print(reason)
end
