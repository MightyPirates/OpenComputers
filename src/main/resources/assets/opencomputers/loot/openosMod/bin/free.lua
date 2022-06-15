local computer = require("computer")
local su = require("superUtiles")

local total = computer.totalMemory()
local max = 0
for _=1,40 do
  max = math.max(max, computer.freeMemory())
  os.sleep(0) -- invokes gc
end

total = math.floor(total / 1024)
max = math.floor(max / 1024)

print("Total", tostring(total) .. "k")
print("Used", tostring(total - max) .. "k", tostring(math.floor(su.mapClip(total - max, 0, math.floor(computer.totalMemory() / 1024), 0, 100))) .. "%")
print("Free", tostring(max) .. "k", tostring(math.floor(su.mapClip(max, 0, math.floor(computer.totalMemory() / 1024), 0, 100))) .. "%")