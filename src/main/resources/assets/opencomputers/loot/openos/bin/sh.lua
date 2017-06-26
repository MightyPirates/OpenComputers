local event = require("event")
local shell = require("shell")
local tty = require("tty")
local text = require("text")
local sh = require("sh")

local input = table.pack(...)
local args = shell.parse(select(3,table.unpack(input)))
if input[2] then
  table.insert(args, 1, input[2])
end

local history = {hint = sh.hintHandler}
shell.prime()
local update_gpu = io.output().tty
local interactive = io.input().tty
local foreground

if #args == 0 then
  while true do
    if update_gpu then
      while not tty.isAvailable() do
        event.pull("term_available")
      end
      if not foreground and interactive then -- first time run AND interactive
        dofile("/etc/profile.lua")
      end
      foreground = tty.gpu().setForeground(0xFF0000)
      io.write(sh.expand(os.getenv("PS1") or "$ "))
      tty.gpu().setForeground(foreground)
      tty.setCursorBlink(true)
    end
    local command = tty.read(history)
    if command then
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = sh.execute(_ENV, command)
        if update_gpu and tty.getCursor() > 1 then
          io.write("\n")
        end
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    elseif command == nil then -- command==false is a soft interrupt, ignore it
      if interactive then
        io.write("exit\n") -- pipe closed
      end
      return -- eof
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
