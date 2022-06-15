local screenShot = require("screenShot")
local term = require("term")
local su = require("superUtiles")
local computer = require("computer")

--------------------------------------------

local lib = {}

function lib.splash(text)
    checkArg(1, text, "string")

    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()
    local rx, ry = gpu.getResolution()

    local reset_gpu = su.saveGpu()
    local backimage = screenShot.pull(1, 1, rx, 4)
    gpu.setBackground(su.selectColor(nil, 0xFF0000, nil, true))
    gpu.setForeground(su.selectColor(nil, 0xFFFFFF, nil, false))
    gpu.fill(1, 1, rx, 4, " ")
    gpu.fill(1, 1, rx, 1, "-")
    gpu.fill(1, 4, rx, 1, "-")
    gpu.set(1, 2, text)

    for i = 3, 1, -1 do
        gpu.set(1, 3, "Timeout " .. tostring(i) .. " seconds")
        computer.delay(1)
    end

    gpu.set(1, 3, "Press enter to continue")

    while true do
        local eventName, uuid, char, code = computer.rawPullSignal()
        if eventName == "key_down" and uuid == keyboard and code == 28 then
            break
        end
    end

    reset_gpu()
    backimage()
end

return lib