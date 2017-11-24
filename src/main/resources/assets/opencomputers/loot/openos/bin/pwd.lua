local shell = require("shell")
local fs = require("filesystem")
local _,op = shell.parse(...)

local path, why = shell.getWorkingDirectory(), ""
if op.P then
  path, why = fs.realPath(path)
end
if not path then
  io.stderr:write(string.format("error retrieving current directory: %s", why))
  os.exit(1)
end

io.write(path, "\n")
