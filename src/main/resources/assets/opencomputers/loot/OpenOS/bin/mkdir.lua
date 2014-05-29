local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: mkdir <dirname1> [<dirname2> [...]]")
  return
end

for i = 1, #args do
  local path = shell.resolve(args[i])
  local result, reason = fs.makeDirectory(path)
  if not result then
    if not reason then
      if fs.exists(path) then
        reason = "file or folder with that name already exists"
      else
        reason = "unknown reason"
      end
    end
    io.stderr:write(path .. ": " .. reason .. "\n")
  end
end
