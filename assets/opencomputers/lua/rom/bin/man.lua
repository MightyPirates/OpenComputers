local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: man topic\n")
  io.write("Where `topic` will usually be the name of a program or library.")
  return
end

local topic = args[1]
local path = "/usr/man/" .. topic
if fs.exists(path) and not fs.isDirectory(path) then
  os.execute("more " .. path)
else
  io.write("No manual entry for " .. topic)
end