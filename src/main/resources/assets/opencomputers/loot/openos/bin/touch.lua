--[[Lua implementation of the UN*X touch command--]]
local shell = require("shell")
local fs =  require("filesystem")

local args, options = shell.parse(...)

local function usage()
  print(
[[Usage: touch [OPTION]... FILE...
Update the modification times of each FILE to the current time.
A FILE argument that does not exist is created empty, unless -c is supplied.

  -c, --no-create    do not create any files
      --help         display this help and exit]])
end

if options.help then
  usage()
  return 0
elseif #args == 0 then
  io.stderr:write("touch: missing operand\n")
  return 1
end

options.c = options.c or options["no-create"]
local errors = 0

for _,arg in ipairs(args) do
  local path = shell.resolve(arg)

  if fs.isDirectory(path) then
    io.stderr:write(string.format("`%s' ignored: directories not supported\n", arg))
  elseif fs.exists(path) or not options.c then
    local f, reason = io.open(path, "a")
    if not f then
      io.stderr:write(string.format("touch: cannot touch `%s': permission denied\n", arg))
      errors = 1
    else
      f:close()
    end
  end
end

return errors
