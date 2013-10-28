local args, options = shell.parse(...)

if #args < 1 then
  print("Usage: umount [-a] <mount>")
  print(" -a  Remove any mounts by file system label or address instead of by path. Note that the address may be abbreviated.")
  return
end

local proxy, reason
if options.a then
  proxy, reason = fs.proxy(args[1])
  if proxy then
    proxy = proxy.address
  end
else
  local path = shell.resolve(args[1])
  proxy, reason = fs.get(path)
  if proxy then
    proxy = reason -- = path
    if proxy ~= path then
      print("not a mount point")
      return
    end
  end
end
if not proxy then
  print(reason)
  return
end

if not fs.umount(proxy) then
  print("nothing to unmount here")
end
