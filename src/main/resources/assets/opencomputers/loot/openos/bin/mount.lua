local fs = require("filesystem")
local shell = require("shell")

local function usage()
  io.stderr:write([==[
Usage: mount [OPTIONS] [device] [path]")
  If no args are given, all current mount points are printed.
  <Options> Note that multiple options can be used together
  -r, --ro    Mount the filesystem read only
      --bind  Create a mount bind point, folder to folder
  <Args>
  device      Specify filesystem device by one of:
              a. label
              b. address (can be abbreviated)
              c. folder path (requires --bind)
  path        Target folder path to mount to

See `man mount` for more details
  ]==])
  os.exit(1)
end

-- smart parse, follow arg after -o
local args, opts = shell.parse(...)
opts.readonly = opts.r or opts.readonly

if opts.h or opts.help then
  usage()
end

local function print_mounts()
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

  local smounts = {}
  for key,value in pairs(mounts) do
    smounts[#smounts+1] = {key, value}
  end
  table.sort(smounts, function(a,b) return a[1] < b[1] end)

  for _, dev in ipairs(smounts) do
    local dev_path, dev_mounts = table.unpack(dev)
    for _,device in ipairs(dev_mounts) do
      local rw_ro = "(" .. device.rw_ro .. ")"
      local fs_label = "\"" .. device.fs_label .. "\""

      io.write(string.format("%-8s on %-10s %s %s\n",
        dev_path:sub(1,8),
        device.mount_path,
        rw_ro,
        fs_label))
    end
  end
end

local function do_mount()
  -- bind converts a path to a proxy
  local proxy, reason = fs.proxy(args[1], opts)
  if not proxy then
    io.stderr:write("Failed to mount: ", tostring(reason), "\n")
    os.exit(1)
  end

  local result, mount_failure = fs.mount(proxy, shell.resolve(args[2]))
  if not result then
    io.stderr:write(mount_failure, "\n")
    os.exit(2) -- error code
  end
end

if #args == 0 then
  if next(opts) then
    io.stderr:write("Missing argument\n")
    usage()
  else
    print_mounts()
  end
elseif #args == 2 then
  do_mount()
else
  io.stderr:write("wrong number of arguments: ", #args, "\n")
  usage()
end
