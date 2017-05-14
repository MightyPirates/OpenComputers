local computer = require("computer")
local total = computer.totalMemory()
local max = 0
for i=1,40 do
  max = math.max(max, computer.freeMemory())
  os.sleep(0) -- invokes gc
end
print(string.format("Total%12d\nUsed%13d\nFree%13d", total, total - max, max))
