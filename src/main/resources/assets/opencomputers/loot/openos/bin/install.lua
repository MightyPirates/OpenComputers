local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local unicode = require("unicode")
local shell = require("shell")
local tx = require("transforms")
local text = require("text")

local lib = {}

lib.args, lib.options = shell.parse(...)

lib.sources = {}
lib.targets = {}

lib.source_label = lib.args[1]

lib.stdout = io.stdout
lib.stderr = io.stderr
lib.stdin = io.stdin
lib.exit = os.exit

if lib.options.help then
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
  lib.stderr:write("no root filesystem, aborting\n");
  lib.exit(1)
end

function lib.up_deprecate(old_key, new_key)
  if lib.options[new_key] == nil then
    lib.options[new_key] = lib.options[old_key]
  end
  lib.options[old_key] = nil
end

function lib.cleanPath(path)
  if path then
    local rpath = shell.resolve(path)
    if fs.isDirectory(rpath) then
      return fs.canonical(rpath):gsub("/+$", "") .. '/'
    end
  end
end

function lib.load_options()
  lib.up_deprecate('noboot', 'nosetboot')
  lib.up_deprecate('nolabelset', 'nosetlabel')
  lib.up_deprecate('name', 'label')

  lib.source_root = lib.cleanPath(lib.options.from)
  lib.target_root = lib.cleanPath(lib.options.to)

  lib.source_dir = (lib.options.fromDir or "") .. '/.'
  lib.target_dir = (lib.options.root or lib.options.toDir or "") .. "/."

  lib.update = lib.options.u or lib.options.update

  lib.source_dev = lib.source_root and fs.get(lib.source_root)
  lib.target_dev = lib.target_root and fs.get(lib.target_root)
end

local rootAddress = rootfs.address
-- if the rootfs is read only, it is probably the loot disk!
lib.rootException = rootAddress
if rootfs.isReadOnly() then
  lib.rootException = nil
end

-- this may be OpenOS specific, default to "" in case no /dev mount point
local devfsAddress = (fs.get("/dev/") or {}).address or ""

-- tmp is only valid if specified as an option
local tmpAddress = computer.tmpAddress()

function lib.load(path, env)
  if fs.exists(path) then
    local loader, reason = loadfile(path, "bt", setmetatable(env or {}, {__index=_G}))
    if not loader then
      return nil, reason
    end
    local ok, loaded = pcall(loader)
    return ok and loaded, ok or loaded
  end
end

function lib.validDevice(candidate, exceptions, specified, existing)
  local address = candidate.dev.address

  if tx.first(existing, function(e) return e.dev.address == address end) then
    return
  end

  if specified then
    if type(specified) == "string" and address:find(specified, 1, true) == 1 or specified == candidate.dev then
      return true
    end
  else
    return not tx.find(exceptions, {address})
  end
end

function lib.relevant(candidate, path)
  if not path or fs.get(path) ~= candidate.dev then
    return candidate.path
  end
  return path
end

-- use a single for loop of all filesystems to build the list of candidates of sources and targets
function lib.load_candidates()
  for dev, path in fs.mounts() do
    local candidate = {dev=dev, path=path:gsub("/+$","")..'/'}

    if lib.validDevice(candidate, {devfsAddress, tmpAddress, lib.rootException}, lib.source_dev or lib.options.from, lib.sources) then
      local root_path = lib.relevant(candidate, lib.source_root)
      if (lib.options.from or fs.list(root_path)()) then -- ignore empty sources unless specified
        candidate.prop = lib.load(root_path .. "/.prop") or {}
        if not lib.source_label or lib.source_label:lower() == (candidate.prop.label or candidate.dev.getLabel()):lower() then
          table.insert(lib.sources, candidate)
        end
      end
    end

    -- in case candidate is valid for BOTH, we want a new table
    candidate = {dev=candidate.dev, path=candidate.path} -- but not the prop

    if lib.validDevice(candidate, {devfsAddress, tmpAddress}, lib.target_dev or lib.options.to, lib.targets) then
      if not dev.isReadOnly() then
        table.insert(lib.targets, candidate)
      elseif lib.options.to then
        lib.stderr:write("Cannot install to " .. lib.options.to .. ", it is read only\n")
        lib.exit(1)
        return false -- in lib mode this can be hit
      end
    end
  end

  return true
end

function lib.check_sources()
  if #lib.sources == 0 then
    if lib.source_label then
      lib.stderr:write("No filesystem to matched given label: " .. lib.source_label .. '\n')
    elseif lib.options.from then
      lib.stderr:write("No such filesystem to install from: " .. lib.options.from .. '\n')
    else
      lib.stderr:write("Could not find and available installations\n")
    end
    lib.exit(1)
  end
  return true
end

function lib.check_targets()
  if #lib.targets == 0 then
    if lib.options.to then
      lib.stderr:write("No such filesystem to install to: " .. lib.options.to .. '\n')
    else
      lib.stderr:write("No writable disks found, aborting\n")
    end
    lib.exit(1)
  end
  return true
end

----- For now, I am allowing source==target -- cp can handle it if the user prepares conditions correctly
----- in other words, install doesn't need to filter this scenario:
--if #targets == 1 and #sources == 1 and targets[1] == sources[1] then
--  io.stderr:write("It is not the intent of install to use the same source and target filesystem.\n")
--  return 1
--end

function lib.prompt_select(devs, direction)
  table.sort(devs, function(a, b) return a.path<b.path end)

  local choice = devs[1]
  if #devs > 1 then
    lib.stdout:write("Select the device to install " .. direction .. '\n')

    for i = 1, #devs do
      local src = devs[i]
      local label = src.dev.getLabel()
      if label then
        label = label .. " (" .. src.dev.address:sub(1, 8) .. "...)"
      else
        label = src.dev.address
      end
      lib.stdout:write(i .. ") " .. label .. " at " .. src.path .. '\n')
    end

    lib.stdout:write("Please enter a number between 1 and " .. #devs .. '\n')
    lib.stdout:write("Enter 'q' to cancel the installation: ")
    local choice
    while not choice do
      result = lib.stdin:read()
      if result:sub(1, 1):lower() == "q" then
        lib.exit()
        return false
      end
      local number = tonumber(result)
      if number and number > 0 and number <= #devs then
        choice = devs[number]
      else
        lib.stdout:write("Invalid input, please try again: ")
      end
    end
  end

  -- normally it is helpful to call / the root filesystem
  -- but if rootfs is readonly, then we know we are using rootfs as a source
  -- in which case, it's label takes priority
  choice.display = 
    not choice.dev.isReadOnly() and (choice.path == '/' and "the root filesystem") or
    -- everything has props by this point, except for targets
    (choice.prop or {}).label or
    choice.dev.getLabel() or
    choice.path

  return choice
end

function lib.load_env()
  lib.env =
  {
    from=lib.source_root,
    to=lib.target_root,
    fromDir=lib.source_dir,
    root=lib.target_dir,
    update=lib.options.update,
    label=lib.options.label or lib.source.prop.label,
    setlabel=lib.source.prop.setlabel and not lib.options.nosetlabel,
    setboot=lib.source.prop.setboot and not lib.options.nosetboot,
    reboot=lib.source.prop.reboot and not lib.options.noreboot,
  }
end

function lib.init()
  lib.load_options()
  if not lib.load_candidates() then return false end
  if not lib.check_sources() then return false end
  if not lib.check_targets() then return false end

  lib.source = lib.prompt_select(lib.sources, "from")
  if not lib.source then return false end
  lib.source_root = lib.source_root or lib.source.path

  lib.target = lib.prompt_select(lib.targets, "to")
  if not lib.target then return false end
  lib.target_root = lib.target_root or lib.target.path

  lib.load_env()
  local reason
  lib.installer, reason = lib.load(lib.source_root .. '/.install', {install=lib.env})
  if not lib.installer then
    if reason then
      lib.stderr:write("installer failed to load: " .. tostring(reason) .. '\n')
      lib.exit(1)
      return false
    else
      lib.installer = lib.run
    end
  end

  return true
end

function lib.run()
  local cp = shell.resolve("cp", "lua")
  local cp_options = "-vrx" .. (lib.options.update and "ui" or "")
  local cp_source = (lib.source_root .. lib.source_dir):gsub("/+","/")
  local cp_dest = (lib.target_root .. lib.target_dir):gsub("/+","/")

  io.write("Install " .. lib.source.display .. " to " .. lib.target.display .. "? [Y/n] ")
  local choice = text.trim(lib.stdin:read()):lower()
  if choice == "" then
    choice = "y"
  end
  if choice ~= "y" then
    lib.stdout:write("Installation cancelled\n")
    lib.exit()
    return false
  end

  local message = string.format("Installing %s [%s] to %s [%s]", lib.source.display, cp_source, lib.target.display, cp_dest)
  local cmd = cp .. ' ' .. cp_options .. ' ' .. cp_source .. ' ' .. cp_dest
  lib.stdout:write(message .. '\n')
  lib.stdout:write(cmd .. '\n')
  os.sleep(0.25)

  local result, reason = os.execute(cmd)

  if not result then
    error(reason, 0)
  end

  lib.stdout:write("Installation complete!\n")

  if lib.env.setlabel then
    pcall(lib.target.dev.setLabel, lib.env.label)
  end

  local prereboot = function()end
  if lib.env.setboot then
    prereboot = computer.setBootAddress
  end

  if lib.env.reboot then
    lib.stdout:write("Reboot now? [Y/n] ")
    local result = lib.stdin:read()
    if not result or result == "" or result:sub(1, 1):lower() == "y" then
      prereboot(lib.target.dev.address)
      lib.stdout:write("\nRebooting now!\n")
      computer.shutdown(true)
    end
  end

  lib.stdout:write("Returning to shell.\n")
end

if lib.options.lib  then
  return lib
end

lib.init()
lib.run()
