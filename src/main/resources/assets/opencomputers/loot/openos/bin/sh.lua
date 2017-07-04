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

shell.prime()
local update_gpu = io.output().tty
local interactive = io.input().tty

if #args == 0 then
  while true do
    if update_gpu then
      while not tty.isAvailable() do
        event.pull("term_available")
      end
      if interactive == true then -- first time run AND interactive
        interactive = 0
        tty.setReadHandler({hint = sh.hintHandler})
        dofile("/etc/profile.lua")
      end
      io.write(sh.expand(os.getenv("PS1") or "$ "))
      tty.setCursorBlink(true)
    end
    local command = io.read()
    if command then
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = sh.execute(_ENV, command)
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    elseif not interactive then
      return -- eof
    end
    if update_gpu and tty.getCursor() > 1 then
      io.write("\n")
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
