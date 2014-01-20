os.execute = function(command)
  local shell = require("shell")
  if not command then
    return type(shell) == "table"
  end
  checkArg(1, command, "string")
  local unicode = require("unicode")
  local head, tail = nil, ""
  repeat
    local oldHead = head
    head = command:match("^%S+")
    tail = unicode.sub(command, unicode.len(head) + 1) .. tail
    if head == oldHead then -- say no to infinite recursion, live longer
      command = nil
    else
      command = shell.getAlias(head)
    end
  until command == nil
  local args = {}
  for part in tail:gmatch("%S+") do
    table.insert(args, part)
  end
  return shell.execute(head, _ENV, table.unpack(args))
end

function os.exit()
  error("terminated", 0)
end

function os.remove(...)
  return require("filesystem").remove(...)
end

function os.rename(...)
  return require("filesystem").rename(...)
end

function os.sleep(timeout)
  checkArg(1, timeout, "number", "nil")
  local computer = require("computer")
  local deadline = computer.uptime() + (timeout or 0)
  local event = require("event")
  repeat
    event.pull(deadline - computer.uptime())
  until computer.uptime() >= deadline
end

function os.tmpname()
  local fs = require("filesystem")
  if fs.exists("tmp") then
    for i = 1, 10 do
      local name = "tmp/" .. math.random(1, 0x7FFFFFFF)
      if not fs.exists(name) then
        return name
      end
    end
  end
end
