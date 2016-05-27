local shell = require("shell")
local data = require("data")

local args = shell.parse(...)
if #args == 0 then
  local read = ""
  repeat
    local current = io.read("*a")
    read = read .. current
  until current ~= ""
  io.write(data.deflate(read))
else
  local read = ""
  local file, reason = io.open(shell.resolve(args[1]))
  if not file then
    io.stderr:write(tostring(reason) .. "\n")
    os.exit(false)
  end
  repeat
    local current = file:read("*a")
    read = read .. current
  until current ~= ""
  file:close()
  io.write(data.deflate(read))
end
