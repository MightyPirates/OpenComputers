local su = require("superUtiles")
local component = require("component")

-------------------------------------------

print("DRIVES LIST")

local count = 0
for address in component.list("filesystem") do
    count = count + 1
    local proxy = component.proxy(address)

    print(tostring(count) .. ".", su.getFullInfoParts(address))
end