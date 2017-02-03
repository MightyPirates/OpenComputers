local fs = require("filesystem")
local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: mkdir <dirname1> [<dirname2> [...]]\n")
  return 1
end

local ec = 0
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
    io.stderr:write("mkdir: cannot create directory '" .. tostring(args[i]) .. "': " .. reason .. "\n")
    ec = 1
  end
end

return ec
