local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local unicode = require("unicode")

local function env()
  -- copy parent env when first requested; easiest way to keep things
  -- like number of env vars trivial (#vars).
  local data = require("process").info().data
  --[[ TODO breaking change; will require set to be a shell built-in and
            may break other programs relying on setenv being global.
  if not rawget(data, "vars") then
    local vars = {}
    for k, v in pairs(data.vars or {}) do
      vars[k] = v
    end
    data.vars = vars
  end
  --]]
  data.vars = data.vars or {}
  return data.vars
end

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
    return #env()
  elseif varname ~= nil then
    return env()[varname]
  else
    return env()
  end
end

function os.setenv(varname, value)
  checkArg(1, varname, "string", "number")
  if value == nil then
    env()[varname] = nil
  else
    local success, val = pcall(tostring, value)
    if success then
      env()[varname] = val
      return env()[varname]
    else
      return nil, val
    end
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
  local path = os.getenv("TMPDIR") or "/tmp"
  if fs.exists(path) then
    for i = 1, 10 do
      local name = fs.concat(path, tostring(math.random(1, 0x7FFFFFFF)))
      if not fs.exists(name) then
        return name
      end
    end
  end
end

os.setenv("EDITOR", "/bin/edit")
os.setenv("HISTSIZE", "10")
os.setenv("HOME", "/home")
os.setenv("IFS", " ")
os.setenv("MANPATH", "/usr/man:.")
os.setenv("PAGER", "/bin/more")
os.setenv("PATH", "/bin:/usr/bin:/home/bin:.")
os.setenv("PS1", "$PWD# ")
os.setenv("PWD", "/")
os.setenv("SHELL", "/bin/sh")
os.setenv("TMP", "/tmp") -- Deprecated
os.setenv("TMPDIR", "/tmp")

if computer.tmpAddress() then
  fs.mount(computer.tmpAddress(), os.getenv("TMPDIR") or "/tmp")
end
