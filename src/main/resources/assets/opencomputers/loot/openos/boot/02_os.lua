local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local info = require("process").info

os.execute = function(command)
  if not command then
    return type(shell) == "table"
  end
  return shell.execute(command)
end

function os.exit(code)
  error({reason="terminated", code=code}, 0)
end

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

os.remove = fs.remove
os.rename = fs.rename

function os.sleep(timeout)
  checkArg(1, timeout, "number", "nil")
  local deadline = computer.uptime() + (timeout or 0)
  repeat
    event.pull(deadline - computer.uptime())
  until computer.uptime() >= deadline
end

function os.tmpname()
  local path = os.getenv("TMPDIR") or "/tmp"
  if fs.exists(path) then
    for _ = 1, 10 do
      local name = fs.concat(path, tostring(math.random(1, 0x7FFFFFFF)))
      if not fs.exists(name) then
        return name
      end
    end
  end
end

os.setenv("PATH", "/bin:/usr/bin:/home/bin:.")
os.setenv("TMP", "/tmp") -- Deprecated
os.setenv("TMPDIR", "/tmp")

if computer.tmpAddress() then
  fs.mount(computer.tmpAddress(), os.getenv("TMPDIR") or "/tmp")
end
