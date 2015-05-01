--[[Lua implementation of the UN*X yes command--]]
local shell = require("shell")

local args, options = shell.parse(...)

if options.V or options.version then
  io.write("yes v:1.0-2\n")
  io.write("Inspired by functionality of yes from GNU coreutils\n")
  return 0
end

if options.h or options.help then
  io.write("Usage: yes [string]...\n")
  io.write("OR:    yes [-V/h]\n")
  io.write("\n")
  io.write("yes prints the command line arguments, or 'y', until is killed.\n")
  io.write("\n")
  io.write("Options:\n")
  io.write("	-V, --version	Version\n")
  io.write("	-h, --help  	This help\n")
  return 0
end

-- If there are no arguments, print 'y' and new line, if there is print it.
if #args == 0 then
  while pcall(io.write, "y\n") do
    os.sleep(0)
  end
else
  repeat
    local ok = true
    for i=1, #args, 1 do
      ok = ok and pcall(io.write, args[i], " ")
    end
    pcall(io.write, "\n")
    os.sleep(0)
  until not ok
end
return 0
