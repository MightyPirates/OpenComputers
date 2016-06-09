local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local unicode = require("unicode")
local shell = require("shell")
local tx = require("transforms")
local text = require("text")

local args, options = shell.parse(...)

local sources = {}
local targets = {}

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
The following only pertain when .osprop exists
  --nolabelset       do not label target
  --name             override label from .osprop
  --noboot           do not use target for boot
  --noreboot         do not reboot after install]])
  return nil -- exit success
end

local rootfs = fs.get("/")
if not rootfs then
  io.stderr:write("no root filesystem, aborting\n");
  return 1
end

local rootAddress = rootfs.address
-- if the rootfs is read only, it is probably the loot disk!
local rootException = rootAddress
if rootfs.isReadOnly() then
  rootException = nil
end

-- this may be OpenOS specific, default to "" in case no /dev mount point
local devfsAddress = (fs.get("/dev/") or {}).address or ""

-- tmp is only valid if specified as an option
local tmpAddress = computer.tmpAddress()

local fromAddress = options.from
local toAddress = options.to
local fromDir = (options.fromDir or "") .. '/.'
local root = (options.root or options.toDir or "") .. "/."
options.update = options.u or options.update

local function cleanPath(path)
  if path then
    local rpath = shell.resolve(path)
    if fs.isDirectory(rpath) then
      return fs.canonical(rpath):gsub("/+$", "") .. '/'
    end
  end
  return path
end

fromAddress = cleanPath(fromAddress)
toAddress = cleanPath(toAddress)

local function validDevice(candidate, exceptions, specified, existing)
  local address = candidate.dev.address

  if tx.first(existing, function(e) return e.dev.address == address end) then
    return
  end

  local path = candidate.path
  if specified then
    return address:find(specified, 1, true) == 1 or specified == path
  else
    return not tx.find(exceptions, {address})
  end
end

-- use a single for loop of all filesystems to build the list of candidates of sources and targets
for dev, path in fs.mounts() do
  local candidate = {dev=dev, path=path}

  if validDevice(candidate, {devfsAddress, tmpAddress, rootException}, fromAddress, sources) then
    if fromAddress or fs.list(path)() then
      table.insert(sources, candidate)
    end
  end

  if validDevice(candidate, {devfsAddress, tmpAddress}, toAddress, targets) then
    if not dev.isReadOnly() then
      table.insert(targets, candidate)
    elseif toAddress then
      io.stderr:write("Cannot install to " .. toAddress .. ", it is read only\n")
      return 1
    end
  end
end

if fromAddress and #sources == 0 then
  io.stderr:write("No such filesystem to install from: " .. fromAddress .. "\n")
  return 1
end

if #targets == 0 then
  if toAddress then
    io.stderr:write("No such filesystem to install to: " .. toAddress .. "\n")
  else
    io.stderr:write("No writable disks found, aborting\n")
  end
  return 1
end

----- For now, I am allowing source==target -- cp can handle it if the user prepares conditions correctly
----- in other words, install doesn't need to filter this scenario:
--if #targets == 1 and #sources == 1 and targets[1] == sources[1] then
--  io.stderr:write("It is not the intent of install to use the same source and target filesystem.\n")
--  return 1
--end

local function prompt_select(devs, direction)

  local choice = devs[1]
  if #devs > 1 then
    print("Select the device to install " .. direction)

    for i = 1, #devs do
      local src = devs[i]
      local label = src.dev.getLabel()
      if label then
        label = label .. " (" .. src.dev.address:sub(1, 8) .. "...)"
      else
        label = src.dev.address
      end
      print(i .. ") " .. label .. " at " .. src.path)
    end

    print("Please enter a number between 1 and " .. #devs)
    io.write("Enter 'q' to cancel the installation: ")
    local choice
    while not choice do
      result = io.read()
      if result:sub(1, 1):lower() == "q" then
        os.exit()
      end
      local number = tonumber(result)
      if number and number > 0 and number <= #devs then
        choice = devs[number]
      else
        io.write("Invalid input, please try again: ")
      end
    end
  end

  choice.display = (choice.path == '/' and "the root filesystem") or choice.dev.getLabel() or choice.path

  if #devs == 1 then
    print("Selecting " .. choice.display .. " (only option)")
  end

  return choice
end

table.sort(sources, function(a, b) return a.path<b.path end)
table.sort(targets, function(a, b) return a.path<b.path end)

local source = prompt_select(sources, "from")
local target = prompt_select(targets, "to")

-- load .osprop (optional) settings
local osprop = nil
if fs.exists(source.path .. ".osprop") then
  local osprop_data, reason = loadfile(source.path .. ".osprop", "bt", setmetatable({}, {__index=_G}))
  if not osprop_data then
    io.stderr:write("Failed to load .osprop: " .. tostring(reason) .. '\n')
    return 1
  end
  osprop = osprop_data()
  options.name = options.name or osprop.name
  source.display = options.name or source.display
end

-- if .lootprop exists
if fs.exists(source.path .. ".lootprop") then
  local env = setmetatable(
  {
    lootprop =
    {
      from=source.path,
      to=target.path,
      fromDir=fromDir,
      root=root,
      update=options.update,
      nolabelset=options.nolabelset,
      name=options.name,
      noboot=options.noboot,
      noreboot=options.noreboot,
    }
  }, {__index=_G})
  local lootprop, reason = loadfile(source.path .. ".lootprop", "bt", env)
  if not lootprop then
    io.stderr:write("Failed to load .lootprop: " .. tostring(reason) .. '\n')
    return 1
  end
  return lootprop()
end

local cp = shell.resolve("cp", "lua")
local cp_options = "-vrx" .. (options.update and "ui" or "")
local cp_source = (source.path .. fromDir):gsub("/+","/")
local cp_dest = (target.path .. root):gsub("/+","/")

io.write("Install " .. source.display .. " to " .. target.display .. "? [Y/n] ")
local choice = text.trim(io.read()):lower()
if choice == "" then
  choice = "y"
end
if choice ~= "y" then
  print("Installation cancelled")
  return
end

local message = string.format("Installing %s [%s] to %s [%s]", source.display, cp_source, target.display, cp_dest)
local cmd = cp .. ' ' .. cp_options .. ' ' .. cp_source .. ' ' .. cp_dest
print(message)
print(cmd)
os.sleep(0.25)

local result, reason = os.execute(cmd)

if not result then
  error(reason, 0)
end

if osprop then
  if not options.nolabelset then
    pcall(target.dev.setLabel, options.name)
  end
  if not options.noreboot then
    io.write("All done! " .. ((not options.noboot) and "Set as boot device and r" or "R") .. "eboot now? [Y/n] ")
    local result = io.read()
    if not result or result == "" or result:sub(1, 1):lower() == "y" then
      if not options.noboot then computer.setBootAddress(target.dev.address)end
      io.write("\nRebooting now!\n")
      computer.shutdown(true)
    end
  end
end
io.write("Returning to shell.\n")
