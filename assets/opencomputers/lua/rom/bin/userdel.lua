local args = shell.parse(...)
if #args < 1 then
  print("Usage: userdel <name>")
  return
end

if not os.removeUser(args[1]) then
  print("no such user")
end
