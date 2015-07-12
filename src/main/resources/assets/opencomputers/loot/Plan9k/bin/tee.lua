local args = {...}

local f = io.open(args[1], "a")

while true do
    local data = io.read(1)
    if not data then
        f:close()
        return
    end
    if io.input().remaining() > 0 then
        data = data .. io.read(io.input().remaining())
    end
    f:write(data)
    f:flush()
    io.write(data)
end
