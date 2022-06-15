--[[Lua implementation of the UN*X yes command--]]
local shell = require("shell")

local args, options = shell.parse(...)

if options.V or options.version then
  io.write("yes v:1.0-3\n")
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

local msg = #args == 0 and 'y' or table.concat(args, ' ')
msg = msg .. '\n'

while io.write(msg) do
  if io.stdout.tty then
    os.sleep(0)
  end
end
return 0
