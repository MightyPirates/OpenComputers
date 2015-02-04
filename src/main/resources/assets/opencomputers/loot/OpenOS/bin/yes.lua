--[[Lua implementation of the UN*X yes command--]]
local shell = require("shell")

local args, options = shell.parse(...)

-- Version option as in GNU coreutils, but in ther case it is "--version"
if options.V then
  io.write("yes v:1.0-2\n")
  io.write("Inspired by functionality of yes from GNU coreutils\n")
  return 0
end
-- Help option as in GNU coreutils, but in ther case it is "--help"
if options.h then
  io.write("Usage: yes [string]...\n")
  io.write("OR:    yes [-V/h]\n")
  io.write("\n")
  io.write("yes prints the command line arguments, or 'y', until is killed.\n")
  io.write("\n")
  io.write("Options:\n")
  io.write("	-V	Version\n")
  io.write("	-h	This help\n")
  return 0
end

-- If there are no arguments, print 'y' and new line, if there is print it.
if #args == 0 then
  while ( true )
  do
    io.write("y\n")
    os.sleep(0)
  end
else
  while ( true )
  do
    for i=1, #args, 1
    do
      io.write(args[i], " ")
    end
    io.write("\n")
    os.sleep(0)
  end
end
return 0
