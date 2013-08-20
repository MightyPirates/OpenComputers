--[[
  Basic OS functionality, such as launching new programs and loading drivers.

  This is called as the main coroutine by the computer. If this throws, the
  computer crashes. If this returns, the computer is considered powered off.
]]

--[[
function _G.os.execute(callback)
  if type(callback) == "string" then
    -- Check if we have a file system
  else
  end
end

function _G.os.exit()
  coroutine.yield("terminate")
end
]]

local i = 1
while true do
  i = i + 1
  coroutine.yield(i)
end