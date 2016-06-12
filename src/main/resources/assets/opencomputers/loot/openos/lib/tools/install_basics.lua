local computer = require("computer")
local shell = require("shell")
local component = require("component")
local event = require("event")
local fs = require("filesystem")
local unicode = require("unicode")
local text = require("text")

local write = io.write
local read = io.read

local args, options = shell.parse(...)

options.sources = {}
options.targets = {}
options.source_label = args[1]

local root_exception

if options.help then
  print([[Usage: install [OPTION]...
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
  --noreboot         do not reboot after install]])
  return nil -- exit success
end

local rootfs = fs.get("/")
if not rootfs then
  io.stderr:write("no root filesystem, aborting\n");
  os.exit(1)
end

local function up_deprecate(old_key, new_key)
  if options[new_key] == nil then
    options[new_key] = options[old_key]
  end
  options[old_key] = nil
end

local function cleanPath(path)
  if path then
    local rpath = shell.resolve(path)
    if fs.isDirectory(rpath) then
      return fs.canonical(rpath) .. '/'
    end
  end
end

local rootAddress = rootfs.address
-- if the rootfs is read only, it is probably the loot disk!
root_exception = rootAddress
if rootfs.isReadOnly() then
  root_exception = nil
end

-- this may be OpenOS specific, default to "" in case no /dev mount point
local devfsAddress = (fs.get("/dev/") or {}).address or ""

-- tmp is only valid if specified as an option
local tmpAddress = computer.tmpAddress()

----- For now, I am allowing source==target -- cp can handle it if the user prepares conditions correctly
----- in other words, install doesn't need to filter this scenario:
--if #options.targets == 1 and #options.sources == 1 and options.targets[1] == options.sources[1] then
--  io.stderr:write("It is not the intent of install to use the same source and target filesystem.\n")
--  return 1
--end

------ load options
up_deprecate('noboot', 'nosetboot')
up_deprecate('nolabelset', 'nosetlabel')
up_deprecate('name', 'label')

options.source_root = cleanPath(options.from)
options.target_root = cleanPath(options.to)

options.target_dir = fs.canonical(options.root or options.toDir or "")

options.update = options.u or options.update

local function path_to_dev(path)
  return path and fs.isDirectory(path) and not fs.isLink(path) and fs.get(path)
end

local source_dev = path_to_dev(options.source_root)
local target_dev = path_to_dev(options.target_root)

-- use a single for loop of all filesystems to build the list of candidates of sources and targets
local function validDevice(candidate, exceptions, specified, existing)
  local address = candidate.dev.address

  for _,e in ipairs(existing) do
    if e.dev.address == address then
      return
    end
  end

  if specified then
    if address:find(specified, 1, true) == 1 then
      return true
    end
  else
    for _,e in ipairs(exceptions) do
      if e == address then
        return
      end
    end
    return true
  end
end

for dev, path in fs.mounts() do
  local candidate = {dev=dev, path=path:gsub("/+$","")..'/'}

  if validDevice(candidate, {devfsAddress, tmpAddress, root_exception}, source_dev and source_dev.address or options.from, options.sources) then
    -- root path is either the command line path given for this dev or its natural mount point
    local root_path = source_dev == dev and options.source_root or path
    if (options.from or fs.list(root_path)()) then -- ignore empty sources unless specified
      local prop = fs.open(root_path .. '/.prop')
      if prop then
        local prop_data = prop:read(math.huge)
        prop:close()
        prop = prop_data
      end
      candidate.prop = prop and load('return ' .. prop)() or {}
      if not options.source_label or options.source_label:lower() == (candidate.prop.label or dev.getLabel()):lower() then
        table.insert(options.sources, candidate)
      end
    end
  end

  -- in case candidate is valid for BOTH, we want a new table
  candidate = {dev=candidate.dev, path=candidate.path} -- but not the prop

  if validDevice(candidate, {devfsAddress, tmpAddress}, target_dev and target_dev.address or options.to, options.targets) then
    if not dev.isReadOnly() then
      table.insert(options.targets, candidate)
    elseif options.to then
      io.stderr:write("Cannot install to " .. options.to .. ", it is read only\n")
      os.exit(1)
    end
  end
end

local source = options.sources[1]
local target = options.targets[1]

if #options.sources ~= 1 or #options.targets ~= 1 then
  source, target = loadfile("/lib/tools/install_utils.lua", "bt", _G)('select', options)
end

if not source then return end
options.source_root = options.source_root or source.path

if not target then return end
options.target_root = options.target_root or target.path

-- now that source is selected, we can update options
options.label      = options.label or source.prop.label
options.setlabel   = source.prop.setlabel and not options.nosetlabel
options.setboot    = source.prop.setboot and not options.nosetboot
options.reboot     = source.prop.reboot and not options.noreboot
options.source_dir = fs.canonical(source.prop.fromDir or options.fromDir or "") .. '/.'

local installer_path = options.source_root .. "/.install"
if fs.exists(installer_path) then
  return loadfile("/lib/tools/install_utils.lua", "bt", _G)('install', options)
end

local cp_args =
{
  "-vrx" .. (options.update and "ui" or ""),
  options.source_root .. options.source_dir,
  options.target_root .. options.target_dir
}

local source_display = (source.prop or {}).label or source.dev.getLabel() or source.path
local special_target = ""
if #options.targets > 1 or options.to then
  special_target = " to " .. cp_args[3]
end
io.write("Install " .. source_display .. special_target .. "? [Y/n] ")
local choice = read():lower()
if choice ~= "y" and choice ~= "" then
  write("Installation cancelled\n")
  os.exit()
end

return
{
  setlabel = options.setlabel,
  label = options.label,
  setboot = options.setboot,
  reboot = options.reboot,
  target = target,
  cp_args = cp_args,
}
