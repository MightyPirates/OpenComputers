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
  local status, reason = xpcall(function()
    repeat
      local line = file:read("*L")
      if line then
        sh.execute(nil, line)
      end
    until not line
  end, function(msg) return {msg, debug.traceback()} end)

  file:close()
  if not status and reason then assert(false, tostring(reason[1]) .."\n".. tostring(reason[2])) end
end
