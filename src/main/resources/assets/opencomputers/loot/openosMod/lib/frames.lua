local term = require("term")
local screenShot = require("screenShot")
local su = require("superUtiles")
local event = require("event")
local unicode = require("unicode")
local process = require("process")
local computer = require("computer")
local colorPic = require("colorPic")

-------------------------------------------

local function runCallback(func, exitFunc, ...)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local oldSignal = process.info().data.signal
    process.info().data.signal = function()
        error("windowClosing", 0)
    end

    local tbl = {pcall(func, ...)}

    local oldHookState = event.superHook --save old interrupt state
    event.superHook = false --do not interrupting
    os.sleep() --fix keyboard
    event.superHook = oldHookState --set old interrupt state

    process.info().data.signal = oldSignal
    exitFunc()
    if not tbl[1] then
        if tbl[2] == "windowClosing" then
            oldSignal()
        else
            error(tbl[2], 0)
        end
    end
    return table.unpack(tbl, 2)
end

-------------------------------------------

local frames = {}

function frames.createButton(x, y, sx, sy, text, back, fore, noAutoredraw)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local obj = {}
    function obj.draw()
        gpu.setBackground(back)
        gpu.setForeground(fore)
        gpu.fill(x, y, sx, sy, " ")

        text = unicode.sub(text, 1, sx)
        local cx, cy = ((sx // 2) + x) - (unicode.len(text) // 2), (sy // 2) + y
        gpu.set(cx, cy, text)
    end
    if not noAutoredraw then
        obj.draw()
    end
    function obj.check(...)
        local tbl = {...}
        if tbl[1] == "touch" and tbl[2] == screen and tbl[3] >= x and tbl[4] >= y and tbl[3] < (x + sx) and tbl[4] < (y + sy) then
            return tbl[5]
        end
    end
    return obj
end

function frames.yesno(x, y, sx, sy, text, yesFunc, noFunc)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local oldFrame = screenShot.pull(x, y, sx, sy)
    local savedGpu = su.saveGpu()

    local function exitFunc()
        savedGpu()
        oldFrame()
    end

    gpu.setBackground(0xFFFFFF)
    gpu.setForeground(0)
    gpu.fill(x, y, sx, sy, gpu.getDepth() == 1 and "#" or " ")

    text = unicode.sub(text, 1, sx)
    local cx, cy = ((sx // 2) + x) - (unicode.len(text) // 2), (sy // 3) + y
    gpu.set(cx, cy, text)

    local no = frames.createButton(x, (y + sy) - 1, 5, 1, "no", su.selectColor(nil, 0xFF0000, nil, false), 0xFFFFFF)
    local yes = frames.createButton((x + sx) - 5, (y + sy) - 1, 5, 1, "yes", su.selectColor(nil, 0x00FF00, nil, false), 0xFFFFFF)

    return runCallback(function()
        while true do
            local eventData = {event.pull()}
            if no.check(table.unpack(eventData)) == 0 then
                if noFunc then noFunc() end
                return false
            elseif yes.check(table.unpack(eventData)) == 0 then
                if yesFunc then yesFunc() end
                return true
            end
        end
    end, exitFunc)
end

function frames.pointsMenu(x, y, sx, sy, label, options)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local oldFrame = screenShot.pull(x, y, sx, sy)
    local savedGpu = su.saveGpu()

    local function exitFunc()
        savedGpu()
        oldFrame()
    end

    gpu.setBackground(0xFFFFFF)
    gpu.setForeground(0)
    gpu.fill(x, y, sx, sy, " ")
    gpu.setBackground(su.selectColor(0x666666, 0xAAAAAA, true))

    label = unicode.sub(label, 1, sx)
    local cx = ((sx // 2) + x) - (unicode.len(label) // 2)
    gpu.set(cx, y, label)

    
end

function frames.splash(x, y, sx, sy, text, time, colorFore, colorBack)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()
    local colors = colorPic.getColors()

    local oldFrame = screenShot.pull(x, y, sx, sy)
    local savedGpu = su.saveGpu()

    local function exitFunc()
        savedGpu()
        oldFrame()
    end

    gpu.setBackground(colorBack or su.selectColor(nil, colors.gray, nil, true))
    gpu.setForeground(colorFore or 0)
    gpu.fill(x, y, sx, sy, gpu.getDepth() == 1 and "▒" or " ")

    text = unicode.sub(text, 1, sx)
    local cx, cy = ((sx // 2) + x) - (unicode.len(text) // 2), (sy // 2) + y
    gpu.set(cx, cy, text)

    computer.delay(time or 1)

    exitFunc()
end

return frames