local component = require("component")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: primary <type> [<address>]\n")
  io.write("Note that the address may be abbreviated.\n")
  return 1
end

local componentType = args[1]

if #args > 1 then
  local address = args[2]
  if not component.get(address) then
    io.stderr:write("no component with this address\n")
    return 1
  else
    component.setPrimary(componentType, address)
    os.sleep(0.1) -- allow signals to be processed
  end
end
if component.isAvailable(componentType) then
  io.write(component.getPrimary(componentType).address, "\n")
else
  io.stderr:write("no primary component for this type\n")
  return 1
end
