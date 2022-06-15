local term = require("term")
local unicode = require("unicode")
local event = require("event")
local su = require("superUtiles")

---------------------------------------------

local function invert()
    local gpu = term.gpu()
    gpu.setForeground(gpu.setBackground(gpu.getForeground()))
end

local function setColor(back, fore)
    local gpu = term.gpu()
    gpu.setBackground(back or 0xFFFFFF)
    gpu.setForeground(fore or 0)
end

local function clear(back, fore)
    setColor(back, fore)
    term.clear()
end

local function setText(text, posY)
    local gpu = term.gpu()
    local rx, ry = gpu.getResolution()
    gpu.set(math.ceil((rx / 2) - (unicode.len(text) / 2)), posY, text)
end

---------------------------------------------

local lib = {}

function lib.setStandart()
    local gpu = term.gpu()
    gpu.setBackground(0)
    gpu.setForeground(0xFFFFFF)
end

function lib.menu(label, strs, num, back, fore)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local rx, ry = gpu.getResolution()
    local select = num or 1
    local posY = ((ry // 2) - (#strs // 2) - 1)
    if posY < 0 then posY = 0 end
    while true do
        clear(back, fore)
        local startpos = (select // ry) * ry
        local dy = posY
        if startpos == 0 then
            if gpu.getDepth() == 1 then
                invert()
                setText(label, 1 + dy)
                invert()
            else
                setText(label, 1 + dy)
                setColor(nil, su.selectColor(nil, 0x888888, 0xAAAAAA, false))
            end
        else
            dy = 0
        end
        for i = 1, #strs do
            local pos = (i + 1 + dy) - startpos
            if pos >= 1 and pos <= ry then
                if keyboard and select == i then invert() end
                setText(strs[i], pos)
                if keyboard and select == i then invert() end
            end
        end
        local eventName, uuid, _, code, button = event.pull()
        if eventName == "key_down" and uuid == keyboard then
            if code == 200 and select > 1 then
                select = select - 1
            end
            if code == 208 and select < #strs then
                select = select + 1
            end
            if code == 28 then
                lib.setStandart()
                return select
            end
        elseif eventName == "touch" and uuid == screen and button == 0 then
            code = (code + startpos) - dy
            code = code - 1
            if code >= 1 and code <= #strs then
                lib.setStandart()
                return code
            end
        elseif eventName == "scroll" and uuid == screen then
            if button == 1 and select > 1 then
                select = select - 1
            end
            if button == -1 and select < #strs then
                select = select + 1
            end
        end
    end
end

function lib.yesno(label)
    return lib.menu(label, {"no", "no", "yes", "no"}) == 3
end

function lib.splash(str)
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    clear()
    gpu.set(1, 1, str)
    gpu.set(1, 2, "press enter or touch to continue...")
    while true do
        local eventName, uuid, _, code = event.pull()
        if eventName == "key_down" and uuid == keyboard then
            if code == 28 then
                break
            end
        elseif eventName == "touch" and uuid == screen then
            break
        end
    end
    lib.setStandart()
end

function lib.inputZone(text)
    clear()
    term.write(text..": ")
    local read = io.read()
    lib.setStandart()
    return read
end

return lib