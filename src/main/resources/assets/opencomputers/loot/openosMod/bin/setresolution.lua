local su = require("superUtiles")
local term = require("term")
local fs = require("filesystem")

if not term.isAvailable() then return end

if fs.exists("/etc/resolution.cfg") then
    local gpu = term.gpu()

    local data = su.getFile("/etc/resolution.cfg")
    if data == "reset" then
        os.execute("reset")
    elseif data:sub(1, 3) == "rax" then
        local _, size = table.unpack(su.split(data, ";"))
        if size then
            os.execute("rax " .. tostring(size))
        else
            os.execute("rax")
        end
    elseif data:sub(1, 10) == "resolution" then
        local _, rx, ry = table.unpack(su.split(data, ";"))
        rx = tonumber(rx)
        ry = tonumber(ry)
        if rx and ry then
            if not pcall(gpu.setResolution, rx, ry) then
                os.execute("reset")
            end
        else
            os.execute("reset")
        end
    end
else
    os.execute("reset")
end