local shell = require("shell")

local args, options = shell.parse(...)
if #args == 0 then
  io.write("Usage: rm [-v]  [ [...]]\n")
  io.write(" -v: verbose output.")
  return
end

for i = 1, #args do
  local path = shell.resolve(args[i])
  if not os.remove(path) then
    io.stderr:write(path .. ": no such file, or permission denied\n")
  end
  if options.v then
    io.write("removed '" .. path .. "'\n")
  end
end
