local args = shell.parse(...)
if #args == 0 then
  print("Usage: primary <type> [<address>]")
  print("Note that the address may be abbreviated.")
  return
end

local componentType = args[1]

if #args > 1 then
  local address = args[2]
  if not component.get(address) then
    print("no component with this address")
    return
  else
    component.setPrimary(componentType, address)
    os.sleep(0.1) -- allow signals to be processed
  end
end
if component.isAvailable(componentType) then
  print(component.getPrimary(componentType).address)
else
  print("no primary component for this type")
end
