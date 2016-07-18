local fs = require("filesystem")
local shell = require("shell")
local term = require("term")

local args, ops = shell.parse(...)
if #args == 0 then
  table.insert(args, ".")
end

local path = args[1]

if ops.help then
  print([[Usage: list [path]
  path:
    optional argument (defaults to ./)
  Displays a list of files in the given path with no added formatting
  Intended for low memory systems]])
  return 0
end

local abs_path = path:match("^/") and path or shell.resolve(shell.getWorkingDirectory() .. '/' .. path)

if not fs.exists(abs_path) then
  io.stderr:write("cannot access " .. tostring(path) .. ": No such file or directory\n")
  return 2
end

for path in fs.list(abs_path) do
  print(path)
end
