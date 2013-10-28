local args = shell.parse(...)

if #args == 0 then
  for fs, path in fs.mount() do
    print(fs.address, path)
  end
  return
end

if #args < 2 then
  print("Usage: mount [<fs address> <path>]")
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
local proxy, reason = address and component.proxy(address)
if not proxy then
  print(reason or "no such file system")
  return
end

local result, reason = fs.mount(proxy, args[2])
if not result then
  print(reason)
end
