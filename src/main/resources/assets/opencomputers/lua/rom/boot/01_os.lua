local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local unicode = require("unicode")

local env = {
  HOME="/home",
  MANPATH="/usr/man",
  PATH="/bin:/usr/bin:/home/bin:.",
  PWD="/",
  SHELL="/bin/sh",
  TMP="/tmp"
}

os.execute = function(command)
  if not command then
    return type(shell) == "table"
  end
  return shell.execute(command)
end

function os.exit(code)
  error({reason="terminated", code=code~=false}, 0)
end

function os.getenv(varname)
  if varname == '#' then
    return #env
  elseif varname ~= nil then
    return env[varname]
  else
    return env
  end
end

function os.setenv(varname, value)
  checkArg(1, varname, "string", "number")
  if value == nil then env[varname] = nil
  local success, val = pcall(tostring, value)
  if success then
    env[varname] = val
    return env[varname]
  else
    return nil, val
  end
end

function os.remove(...)
  return fs.remove(...)
end

function os.rename(...)
  return fs.rename(...)
end

function os.sleep(timeout)
  checkArg(1, timeout, "number", "nil")
  local deadline = computer.uptime() + (timeout or 0)
  repeat
    event.pull(deadline - computer.uptime())
  until computer.uptime() >= deadline
end

function os.tmpname()
  if fs.exists("tmp") then
    for i = 1, 10 do
      local name = "/tmp/" .. math.random(1, 0x7FFFFFFF)
      if not fs.exists(name) then
        return name
      end
    end
  end
end
