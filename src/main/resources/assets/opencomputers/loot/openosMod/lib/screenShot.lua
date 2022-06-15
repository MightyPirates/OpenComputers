local term = require("term")
local su = require("superUtiles")

-------------------------------------------

local lib = {}

function lib.pull(x, y, sx, sy)
    local buffer = {}

    local gpu = term.gpu()

    for cx = x, (x + sx) - 1 do
        for cy = y, (y + sy) - 1 do
            buffer[tostring(cx) .. ":" .. tostring(cy)] = {gpu.get(cx, cy)}
        end
    end

    return function()
        local savedGpu = su.saveGpu()
        for k, pixel in pairs(buffer) do
            local x, y = table.unpack(su.split(k, ":"))
            x = tonumber(x)
            y = tonumber(y)
            local char, foreground, background, forePalleteIndex, backPalleteIndex = table.unpack(pixel)
            if forePalleteIndex then
                gpu.setForeground(forePalleteIndex, true)
            else
                gpu.setForeground(foreground)
            end
            if backPalleteIndex then
                gpu.setBackground(backPalleteIndex, true)
            else
                gpu.setBackground(background)
            end
            gpu.set(x, y, char)
        end
        savedGpu()
    end
end

return lib