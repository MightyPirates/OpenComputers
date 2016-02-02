local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: man <topic>\n")
  io.write("Where `topic` will usually be the name of a program or library.\n")
  return 1
end

local topic = args[1]
for path in string.gmatch(os.getenv("MANPATH"), "[^:]+") do
  path = shell.resolve(fs.concat(path, topic), "man")
  if path and fs.exists(path) and not fs.isDirectory(path) then
    os.execute(os.getenv("PAGER") .. " " .. path)
    os.exit()
  end
end
io.stderr:write("No manual entry for " .. topic .. '\n')
return 1
