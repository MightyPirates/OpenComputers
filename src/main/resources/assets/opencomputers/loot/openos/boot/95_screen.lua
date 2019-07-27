-- automatically adjust resolution to fit entire screen
-- made by Nex4rius

local c = require("component")
if c.isAvailable("gpu") and c.isAvailable("screen") then
    local gpu = c.gpu
    local x, y = c.proxy(gpu.getScreen()).getAspectRatio()
    x = x - 0.25
    y = y - 0.25
    local max_x, max_y = gpu.maxResolution()
    local end_x, end_y
    if x == y then
        end_x = max_y * 2
        end_y = max_y
    elseif x > y then
        end_x = max_x
        end_y = max_x / x * y / 2
        if end_y > max_y then
            local v = max_y / end_y
            end_x = end_x * v
            end_y = end_y * v
        end
        end_x = math.ceil(end_x)
        end_y = math.floor(end_y)
    elseif x < y then
        end_x = max_y / y * x * 2
        end_y = max_y
        if end_x > max_x then
            local v = max_x / end_x
            end_x = end_x * v
            end_y = end_y * v
        end
        end_x = math.floor(end_x)
        end_y = math.ceil(end_y)
    end
    gpu.setResolution(end_x, end_y)
end
