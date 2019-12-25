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
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    elseif command == nil then -- false only means the input was interrupted
      return -- eof
    end
  end
else
  -- execute command.
  local cargs = table.pack(...)
  -- sh can run as a shell command (no env table)
  local cenv = _ENV
  local cargsStart = 1
  if type(cargs[1]) == "table" then
    -- sh can also run as a manually started process (see /bin/source.lua)
    cenv = cargs[1]
    cargsStart = 2
  end
  local result = table.pack(sh.execute(cenv, table.unpack(cargs, cargsStart)))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end
