local computer = require("computer")
local shell = require("shell")
local fs = require("filesystem")

local args, options = shell.parse(...)

if options.help then
  io.write([[Usage: install [OPTION]...
  --from=ADDR        install filesystem at ADDR
                     default: builds list of
                     candidates and prompts user
  --to=ADDR          same as --from but for target
  --fromDir=PATH     install PATH from source
  --root=PATH        same as --fromDir but target
  --toDir=PATH       same as --root
  -u, --update       update files interactively
  --label            override label from .prop
  --nosetlabel       do not label target
  --nosetboot        do not use target for boot
  --noreboot         do not reboot after install
]])
  return nil -- exit success
end

local utils_path = "/lib/core/install_utils.lua"
local utils

local rootfs = fs.get("/")
if not rootfs then
  io.stderr:write("no root filesystem, aborting\n");
  os.exit(1)
end

local label = args[1]
options.label = label

local source_filter = options.from
local source_filter_dev
if source_filter then
  local from_path = shell.resolve(source_filter)
  if fs.isDirectory(from_path) then
    source_filter_dev = fs.get(from_path)
    source_filter = source_filter_dev.address
    options.from = from_path
  end
end

local target_filter = options.to
local target_filter_dev
if target_filter then
  local to_path = shell.resolve(target_filter)
  if fs.isDirectory(target_filter) then
    target_filter_dev = fs.get(to_path)
    target_filter = target_filter_dev.address
    options.to = to_path
  end
end

local sources = {}
local targets = {}

-- tmpfs is not a candidate unless it is specified

local comps = require("component").list("filesystem")
local devices = {}

-- not all mounts are components, only use components
for dev, path in fs.mounts() do
  if comps[dev.address] then
    local known = devices[dev]
    devices[dev] = known and #known < #path and known or path
  end
end

local dev_dev = fs.get("/dev")
devices[dev_dev == rootfs or dev_dev] = nil
local tmpAddress = computer.tmpAddress()

for dev, path in pairs(devices) do
  local address = dev.address
  local install_path = dev == target_filter_dev and options.to or path
  local specified = target_filter and address:find(target_filter, 1, true) == 1

  if dev.isReadOnly() then
    if specified then
      io.stderr:write("Cannot install to " .. options.to .. ", it is read only\n")
      os.exit(1)
    end
  elseif specified or
    not (source_filter and address:find(source_filter, 1, true) == 1) and -- specified for source
    not target_filter and
    address ~= tmpAddress then
    table.insert(targets, {dev=dev, path=install_path, specified=specified})
  end
end

local target = targets[1]
-- if there is only 1 target, the source selection cannot include it
if #targets == 1 then
  devices[targets[1].dev] = nil
end

for dev, path in pairs(devices) do
  local address = dev.address
  local install_path = dev == source_filter_dev and options.from or path
  local specified = source_filter and address:find(source_filter, 1, true) == 1

  if fs.list(install_path)()
    and (specified or 
      not source_filter and
      address ~= tmpAddress and
      not (address == rootfs.address and not rootfs.isReadOnly())) then
    local prop = {}
    local prop_path = install_path .. "/.prop"
    local prop_file = fs.open(prop_path)
    if prop_file then
      local prop_data = prop_file:read(math.huge)
      prop_file:close()
      local prop_load = load("return " .. prop_data)
      prop = prop_load and prop_load()
      if not prop then
        io.stderr:write("Ignoring " .. path .. " due to malformed prop file\n")
        prop = {ignore = true}
      end
    end
    if not prop.ignore then
      if not label or label:lower() == (prop.label or dev.getLabel() or ""):lower() then
        table.insert(sources, {dev=dev, path=install_path, prop=prop, specified=specified})
      end
    end
  end
end

-- Ask the user to select a source
local source = sources[1]
if #sources ~= 1 then
  utils = loadfile(utils_path, "bt", _G)
  source = utils("select", "sources", options, sources)
end
if not source then return end

options =
{
  from     = source.path .. '/',
  fromDir  = fs.canonical(options.fromDir or source.prop.fromDir or ""),
  root     = fs.canonical(options.root or options.toDir or source.prop.root or ""),
  update   = options.update or options.u,
  label    = source.prop.label or label,
  setlabel = not (options.nosetlabel or options.nolabelset) and source.prop.setlabel,
  setboot  = not (options.nosetboot or options.noboot) and source.prop.setboot,
  reboot   = not options.noreboot and source.prop.reboot,
}
local source_display = options.label or source.dev.getLabel() or source.path

-- Remove the source from the target options
for index,entry in ipairs(targets) do
  if entry.dev == source.dev then
    table.remove(targets, index)
    target = targets[1]
  end
end

-- Ask the user to select a target
if #targets ~= 1 then
    if #sources == 1 then
      io.write(source_display, " selected for install\n")
    end

    utils = utils or loadfile(utils_path, "bt", _G)
  target = utils("select", "targets", options, targets)
end
if not target then return end

options.to       = target.path .. '/'

local cp_args =
{
  "-vrx" .. (options.update and "ui" or ""),
  "--skip=.prop",
  fs.concat(options.from, options.fromDir) .. "/.",
  fs.concat(options.to  , options.root)
}

local special_target = ""
if #targets > 1 or target_filter or source_filter then
  special_target = " to " .. cp_args[4]
end

io.write("Install " .. source_display .. special_target .. "? [Y/n] ")
if not ((io.read() or "n").."y"):match("^%s*[Yy]") then
  io.write("Installation cancelled\n")
  os.exit()
end

local installer_path = options.from .. "/.install"
if fs.exists(installer_path) then
  local installer, reason = loadfile(installer_path, "bt", setmetatable({install=options}, {__index = _G}))
  if not installer then
    io.stderr:write("installer failed to load: " .. tostring(reason) .. '\n')
    os.exit(1)
  end
  os.exit(installer())
end

options.cp_args = cp_args
options.target = target

return options
