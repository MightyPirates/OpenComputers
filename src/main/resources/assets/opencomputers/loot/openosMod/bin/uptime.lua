local computer = require("computer")

local seconds = math.floor(computer.uptime())
local minutes, hours = 0, 0
if seconds >= 60 then
  minutes = math.floor(seconds / 60)
  seconds = seconds % 60
end
if minutes >= 60 then
  hours = math.floor(minutes / 60)
  minutes = minutes % 60
end
io.write(string.format("%02d:%02d:%02d\n", hours, minutes, seconds))
