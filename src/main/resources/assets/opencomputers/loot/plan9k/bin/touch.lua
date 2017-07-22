local fs = require("filesystem")

local args = {...}
local file = args[1]

if not file then 
    print("Usage: touch [file]")
    return
end

if fs.exists(file) then return end

fs.open(file, "w"):close()