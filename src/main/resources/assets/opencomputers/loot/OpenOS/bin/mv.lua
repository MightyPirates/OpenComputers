local fs = require("filesystem")
local shell = require("shell")
local sh = require("sh")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: mv [-f] <from> <to>\n")
  io.write(" -f: overwrite file if it already exists.\n")
  return 1
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end
if fs.exists(to) then
  if not options.f then
    io.stderr:write("target file exists\n")
    return 1
  end
  fs.remove(to)
end

local result, reason
if fs.get(from) == fs.get(to) then -- same filesystem
  result, reason = os.rename(from, to)
else
  result, reason = sh.execute(nil, shell.resolve("cp","lua"), "-r", from, to)
  if result then
    result, reason = sh.execute(nil, shell.resolve("rm","lua"), "-r", from)
  end
end

if not result then
  io.stderr:write((reason or "unknown error")..'\n')
  return 1
end
