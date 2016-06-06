local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: mv [-f]  \n")
  io.write(" -f: overwrite file if it already exists.")
  return
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end
if fs.exists(to) then
  if not options.f then
    io.stderr:write("target file exists")
    return
  end
  fs.remove(to)
end
local result, reason = os.rename(from, to)
if not result then
  io.stderr:write(reason or "unknown error")
end

