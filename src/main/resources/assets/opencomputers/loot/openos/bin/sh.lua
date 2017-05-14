local event = require("event")
local shell = require("shell")
local tty = require("tty")
local text = require("text")
local sh = require("sh")

local input = table.pack(...)
local args, options = shell.parse(select(3,table.unpack(input)))
if input[2] then
  table.insert(args, 1, input[2])
end

local history = {hint = sh.hintHandler}
shell.prime()

if #args == 0 and (io.stdin.tty or options.i) and not options.c then
  -- interactive shell.
  -- source profile
  if not tty.isAvailable() then event.pull("term_available") end
  loadfile(shell.resolve("source","lua"))("/etc/profile")
  while true do
    if not tty.isAvailable() then -- don't clear unless we lost the term
      while not tty.isAvailable() do
        event.pull("term_available")
      end
      tty.clear()
    end
    local gpu = tty.gpu()
    while tty.isAvailable() do
      local foreground = gpu.setForeground(0xFF0000)
      tty.write(sh.expand(os.getenv("PS1") or "$ "))
      gpu.setForeground(foreground)
      tty.setCursorBlink(true)
      local command = tty.read(history)
      if not command then
        if command == false then
          break -- soft interrupt
        end
        io.write("exit\n") -- pipe closed
        return -- eof
      end
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = sh.execute(_ENV, command)
        if tty.getCursor() > 1 then
          print()
        end
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    end
  end
elseif #args == 0 and not io.stdin.tty then
  while true do
    io.write(sh.expand(os.getenv("PS1") or "$ "))
    local command = io.read("*l")
    if not command then
      command = "exit"
      io.write(command,"\n")
    end
    command = text.trim(command)
    if command == "exit" then
      return
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
      end
    end
  end
else
  -- execute command.
  local result = table.pack(sh.execute(...))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end
