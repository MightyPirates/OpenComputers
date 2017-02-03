local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
local argc = #args
    
if argc == 0 then 
  -- for each mount
  local mounts = {}
    
  for proxy,path in fs.mounts() do
    local device = {}

    device.dev_path = proxy.address
    device.mount_path = path
    device.rw_ro = proxy.isReadOnly() and "ro" or "rw"
    device.fs_label = proxy.getLabel() or proxy.address

    mounts[device.dev_path] = mounts[device.dev_path] or {}
    local dev_mounts = mounts[device.dev_path]
    table.insert(dev_mounts, device)
  end
    
  table.sort(mounts)
    
  for dev_path, dev_mounts in pairs(mounts) do
    for _,device in ipairs(dev_mounts) do
      local rw_ro = "(" .. device.rw_ro .. ")"
      local fs_label = "\"" .. device.fs_label .. "\""
            
      io.write(string.format("%s on %-10s %s %s\n",
        dev_path:sub(1,8),
        device.mount_path,
        rw_ro,
        fs_label))
    end
  end
elseif argc ~= 2 then
  print("Usage: mount [<label|address> <path>]")
  print("Note that the address may be abbreviated.")
  return 1 -- error code
else
  local proxy, reason = fs.proxy(args[1])
  if not proxy then
    io.stderr:write(reason,"\n")
    return 1
  end

  local result, reason = fs.mount(proxy, shell.resolve(args[2]))
  if not result then
    io.stderr:write(reason,"\n")
    return 2 -- error code
  end
end
