local shell = require("shell")
local fs = require("filesystem")
local sh = require("sh")

local args, options = shell.parse(...)

if #args ~= 1 then
  io.stderr:write("specify a single file to source\n");
  return 1
end

local file, reason = io.open(args[1], "r")

if not file then
  if not options.q then
    io.stderr:write(string.format("could not source %s because: %s\n", args[1], reason));
  end
  return 1
else
  local status, reason = pcall(function()
    repeat
      local line = file:read("*L")
      if line then
        sh.execute(nil, line)
      end
    until not line
  end)

  file:close()
  assert(status, reason)
end
