local computer = require("computer")
local fs = require("filesystem")
local info = require("process").info
local event = require("event")

function os.getenv(varname)
  local env = info().data.vars
  if not varname then
    return env
  elseif varname == '#' then
    return #env
  end
  return env[varname]
end

function os.setenv(varname, value)
  checkArg(1, varname, "string", "number")
  if value ~= nil then
    value = tostring(value)
  end
  info().data.vars[varname] = value
  return value
end

function os.sleep(timeout)
  checkArg(1, timeout, "number", "nil")
  local deadline = computer.uptime() + (timeout or 0)
  local epsilon = 1e-6
  repeat
      local remaining = deadline - computer.uptime()
      if remaining <= epsilon then break end

      event.pull(math.max(remaining, 0.001))
  until computer.uptime() >= deadline - epsilon
end

os.setenv("PATH", "/bin:/usr/bin:/home/bin:.")
os.setenv("TMP", "/tmp") -- Deprecated
os.setenv("TMPDIR", "/tmp")

if computer.tmpAddress() then
  fs.mount(computer.tmpAddress(), "/tmp")
end

require("package").delay(os, "/lib/core/full_filesystem.lua")
