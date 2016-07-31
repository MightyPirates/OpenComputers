local fs = require("filesystem")
local shell = require("shell")
local sh = require("sh")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: mv [-f] <from> <to>\n")
  io.write(" -f: overwrite file if it already exists.\n")
  return 1
end

local function is_mount(path)
end

local from = args[1] ~= "" and shell.resolve(args[1])
local to = args[2] ~= "" and shell.resolve(args[2])

if not from or not fs.exists(from) then
  io.stderr:write(string.format("No such file or directory: '%s'\n", args[1]))
  return 1
elseif not to then
  io.stderr:write(string.format("Cannot move '%s' to '%s' No such file or directory\n", args[1], args[2]))
  return 1
elseif fs.get(from).isReadOnly() then
  io.stderr:write("cannot remove " .. args[1] .. ", filesystem is readonly\n");
  return 1
elseif fs.get(to).isReadOnly() then
  io.stderr:write("cannot write to " .. args[2] .. ", filesystem is readonly\n");
  return 1
elseif fs.isDirectory(from) then
  local path = fs.canonical(from) .. '/'
  for driver, mount_point in fs.mounts() do
    if path == mount_point then
      io.stderr:write("cannot move " .. args[1] .. ", it is a mount point\n");
      return 1
    end
  end
end

if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end
if fs.exists(to) then
  if not options.f then
    io.stderr:write("target file exists\n")
    return 1
  end
end

local result, reason
if fs.get(from) == fs.get(to) then -- same filesystem
  result, reason = os.rename(from, to)
else
  result, reason = sh.execute(nil, shell.resolve("cp","lua"), "-rf", from, to)
  if result then
    result, reason = sh.execute(nil, shell.resolve("rm","lua"), "-rf", from)
  end
end

if not result then
  io.stderr:write((reason or "unknown error")..'\n')
  return 1
end
