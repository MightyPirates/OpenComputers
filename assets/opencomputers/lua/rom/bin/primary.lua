local args = shell.parse(...)
if #args == 0 then
  print("Usage: primary <type> [<address>]")
  print("Note that the address may be abbreviated.")
  return
end

if #args > 1 then
  if not component.get(args[2]) then
    print("no component with this address")
    return
  else
    component.primary(args[1], nil)
    component.primary(args[1], args[2])
  end
end
if component.isAvailable(args[1]) then
  print(component.primary(args[1]).address)
else
  print("no primary component for this type")
end
