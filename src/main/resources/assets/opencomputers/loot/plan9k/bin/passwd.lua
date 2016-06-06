local component = require "component"
local term = require "term"

if not component.data or not component.data.sha256 then
    print("At least T2 data card is required!")
end

io.write("New password: ")
local new = term.read(nil, nil, nil, "*")
io.write("Type again: ")
if term.read(nil, nil, nil, "*") ~= new then
    print("Passwords do not match")
end

local hmacKey = component.data.random(16)

local hash = component.data.sha256(new, hmacKey)
local f = io.open("/etc/passwd", "w")
f:write(component.data.encode64(string.pack("c16c32", hmacKey, hash)))
f:close()

print("Password updated succesfully")