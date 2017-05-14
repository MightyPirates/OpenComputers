local shell = require("shell")
local args = shell.parse(...)

if #args == 0 then
  args = {"/opt/core/lua_shell.lua"}
end

local script, reason = loadfile(args[1], nil, setmetatable({},{__index=_ENV}))
if not script then
  io.stderr:write(tostring(reason) .. "\n")
  os.exit(false)
end
local result, reason = pcall(script, table.unpack(args, 2))
if not result then
  io.stderr:write(reason, "\n")
  os.exit(false)
end
