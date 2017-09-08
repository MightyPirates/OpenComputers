local fs = require("filesystem")
local shell = require("shell")

local args, ops = shell.parse(...)
if #args == 0 then
  table.insert(args, ".")
end

local arg = args[1]
local path = shell.resolve(arg)

if ops.help then
  io.write([[Usage: list [path]
  path:
    optional argument (defaults to ./)
  Displays a list of files in the given path with no added formatting
  Intended for low memory systems
]])
  return 0
end

local real, why = fs.realPath(path)
if real and not fs.exists(real) then
  why = "no such file or directory"
end
if why then
  io.stderr:write(string.format("cannot access '%s': %s", arg, tostring(why)))
  return 1
end

for item in fs.list(real) do
  io.write(item, '\n')
end
