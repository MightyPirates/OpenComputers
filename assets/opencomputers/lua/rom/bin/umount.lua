local args = shell.parse(...)

if #args < 1 then
  print("Usage: umount <fs address>|<path>")
  print("Note that the address may be abbreviated.")
  return
end

local address
for c in component.list("filesystem") do
  if c:sub(1, args[1]:len()) == args[1] then
    address = c
    break
  end
end
address = address or args[1]

if not fs.umount(address) then
  print("nothing to unmount here")
end
