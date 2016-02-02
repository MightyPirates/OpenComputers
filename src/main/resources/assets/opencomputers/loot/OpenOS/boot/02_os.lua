local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local unicode = require("unicode")
local process = require("process")

local function env()
  return process.info().data.vars
end

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
os.setenv("LS_COLORS",[[{FILE=0xFFFFFF,DIR=0x66CCFF,LINK=0xFFAA00,["*.lua"]=0x00FF00}]])

if computer.tmpAddress() then
  fs.mount(computer.tmpAddress(), os.getenv("TMPDIR") or "/tmp")
end
