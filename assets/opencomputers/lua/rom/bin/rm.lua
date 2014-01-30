local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: rm <filename1> [<filename2> [...]]")
  return
end

for i = 1, #args do
  local path = shell.resolve(args[i])
  if not os.remove(path) then
    io.write(path, ": no such file, or permission denied\n")
  end
end
