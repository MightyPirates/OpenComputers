local shell = require("shell")
local fs = require("filesystem")

local args = shell.parse(...)
local ec = 0
if #args == 0 then
  repeat
    local read = io.read("*L")
    if read then
      io.write(read)
    end
  until not read
else
  for i = 1, #args do
    local arg = args[i]
    if fs.isDirectory(arg) then
      io.stderr:write(string.format('cat %s: Is a directory\n', arg))
      ec = 1
    else
      local file, reason = args[i] == "-" and io.stdin or io.open(shell.resolve(args[i]))
      if not file then
        io.stderr:write(string.format("cat: %s: %s\n",args[i],tostring(reason)))
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
end

return ec
