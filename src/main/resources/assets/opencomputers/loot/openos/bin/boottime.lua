-- print how long it took the system to boot --
local computer = require("computer")

print("OpenOS booted in " .. computer.bootTime() .. " seconds")

return 0
