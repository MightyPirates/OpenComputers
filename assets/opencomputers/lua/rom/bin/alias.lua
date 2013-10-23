local args = shell.parse(...)

if args.n == 0 then
  for name, value in shell.aliases() do
    print(name, value)
  end
elseif #args == 1 then
  local value = shell.alias(args[1])
  if value then
    print(value)
  else
    print("no such alias")
  end
else
  shell.alias(args[1], args[2])
  print("alias created: " .. args[1] .. " -> " .. args[2])
end
