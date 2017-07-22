local shell = require("shell")
local fs = require("filesystem")

local args = shell.parse(...)
local ec = 0
if #args == 0 then
  args = {"-"}
end

for i = 1, #args do
  local arg = args[i]
  if fs.isDirectory(arg) then
    io.stderr:write(string.format('cat %s: Is a directory\n', arg))
    ec = 1
  else
    local file, reason
    if args[i] == "-" then
      file, reason = io.stdin, "missing stdin"
    else
      file, reason = io.open(shell.resolve(args[i]))
    end
    if not file then
      io.stderr:write(string.format("cat: %s: %s\n", args[i], tostring(reason)))
      ec = 1
    else
      repeat
        local line = file:read("*L")
        if line then
          io.write(line)
        end
      until not line
      file:close()
    end
  end
end

return ec
