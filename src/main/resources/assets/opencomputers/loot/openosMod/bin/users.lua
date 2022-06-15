local computer = require("computer")

-------------------------------------------

local count = 0

for _, data in ipairs({computer.users()}) do
    print(data)
    count = count + 1
end

if count == 0 then print("this public computer") end