local args = shell.parse(...)

if #args == 0 then
  for proxy, path in fs.mounts() do
    local label = proxy.getLabel() or proxy.address
    local mode = proxy.isReadOnly() and "ro" or "rw"
    print(string.format("%s on %s (%s)", label, path, mode))
  end
  return
end

if #args < 2 then
  print("Usage: mount [<label|address> <path>]")
  print("Note that the address may be abbreviated.")
  return
end

local proxy, reason = fs.proxy(args[1])
if not proxy then
  print(reason)
  return
end

local result, reason = fs.mount(proxy, shell.resolve(args[2]))
if not result then
  print(reason)
end
