local computer = require("computer")
local total = computer.totalMemory()
local max = 0
for _=1,40 do
  max = math.max(max, computer.freeMemory())
  os.sleep(0) -- invokes gc
end
io.write(string.format("Total%12d\nUsed%13d\nFree%13d\n", total, total - max, max))
