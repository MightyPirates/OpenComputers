local event = require("event")
local shell = require("shell")
local tty = require("tty")
local text = require("text")
local sh = require("sh")

local args, options = shell.parse(...)

shell.prime()
local update_gpu = io.output().tty and not options.c
local needs_profile = io.input().tty
local input_handler = {hint = sh.hintHandler}

if #args == 0 then
  while true do
    if update_gpu then
      while not tty.isAvailable() do
        event.pull("term_available")
      end
      if needs_profile then -- first time run AND interactive
        needs_profile = nil
        dofile("/etc/profile.lua")
      end
      io.write(sh.expand(os.getenv("PS1") or "$ "))
    end
    local command = tty:read(input_handler)
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
    else
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
