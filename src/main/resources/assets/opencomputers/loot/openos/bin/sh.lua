local shell = require("shell")
local tty = require("tty")
local text = require("text")
local sh = require("sh")

local args = shell.parse(...)

shell.prime()

if #args == 0 then
  local has_profile
  local input_handler = {hint = sh.hintHandler}
  while true do
    if io.stdin.tty and io.stdout.tty then
      if not has_profile then -- first time run AND interactive
        has_profile = true
        dofile("/etc/profile.lua")
      end
      if tty.getCursor() > 1 then
        io.write("\n")
      end
      io.write(sh.expand(os.getenv("PS1") or "$ "))
    end
    tty.window.cursor = input_handler
    local command = io.stdin:readLine(false)
    tty.window.cursor = nil
    if command then
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        --luacheck: globals _ENV
        local result, reason = sh.execute(_ENV, command)
        if not result and reason then
          io.stderr:write(tostring(reason), "\n")
        end
      end
    elseif command == nil then -- false only means the input was interrupted
      return -- eof
    end
  end
else
  -- execute command.
  return sh.execute(...)
end
