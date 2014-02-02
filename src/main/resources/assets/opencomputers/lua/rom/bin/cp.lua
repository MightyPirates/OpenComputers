local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: cp [-f] <from> <to>\n")
  io.write(" -f: overwrite file if it already exists.")
  return
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end
if fs.exists(to) and not options.f then
  error("target file exists")
end
local result, reason = fs.copy(from, to)
if not result then
  io.write(reason)
end