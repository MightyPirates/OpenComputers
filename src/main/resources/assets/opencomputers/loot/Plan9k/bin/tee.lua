local args = {...}

local f = io.open(args[1], "a")

while true do
    local data = io.read("*L")
    if not data then
        f:close()
        return
    end
    if io.input().remaining() > 0 then
        data = data .. io.read(io.input().remaining())
    end
    io.write(data)
    f:write(data)
    f:flush()
end
