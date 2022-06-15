local thread = require("thread")
local term = require("term")
local event = require("event")
local keyboardLib = require("keyboard")

-------------------------------------------

local function createCursor()
    local gpu = term.gpu()
    local screen = term.screen()
    local keyboard = term.keyboard()

    local obj = {}
    obj.posX = 1
    obj.posY = 1
    obj.pressedButton = false

    obj.blickState = false
    function obj.draw()
        local char, fore, back = gpu.get(obj.posX, obj.posY)
        gpu.setBackground(0xFFFFFF - back)
        gpu.setForeground(0xFFFFFF - fore)
        gpu.set(obj.posX, obj.posY, char)
        obj.blickState = not obj.blickState
    end

    function obj.setBlick(state)
        if obj.blickState ~= state then
            obj.draw()
        end
    end

    function obj.posCheck()
        local rx, ry = gpu.getResolution()
        local posChange = false
        if obj.posX > rx then
            obj.posX = rx
            posChange = true
        elseif obj.posX < 1 then
            obj.posX = 1
            posChange = true
        end
        if obj.posY > ry then
            obj.posY = ry
            posChange = true
        elseif obj.posY < 1 then
            obj.posY = 1
            posChange = true
        end
        return posChange
    end

    obj.thread = thread.create(function()
        while true do
            obj.draw()
            local eventName, uuid, char, code, nikname = event.pull(0.5, nil, keyboard)
            obj.posCheck()

            if keyboardLib.isControlDown() then
                if eventName == "key_down" then
                    if code == keyboardLib.keys.up then
                        event.push("scroll", screen, obj.posX, obj.posY, 1, nikname)
                    elseif code == keyboardLib.keys.down then
                        event.push("scroll", screen, obj.posX, obj.posY, -1, nikname)
                    end
                end
            else
                if eventName == "key_down" then
                    local posChange = false
                    if code == keyboardLib.keys.up then
                        obj.setBlick(false)
                        obj.posY = obj.posY - 1
                        posChange = true
                    elseif code == keyboardLib.keys.down then
                        obj.setBlick(false)
                        obj.posY = obj.posY + 1
                        posChange = true
                    elseif code == keyboardLib.keys.left then
                        obj.setBlick(false)
                        obj.posX = obj.posX - 1
                        posChange = true
                    elseif code == keyboardLib.keys.right then
                        obj.setBlick(false)
                        obj.posX = obj.posX + 1
                        posChange = true
                    end
                    if obj.posCheck() then
                        posChange = false
                    end

                    if posChange and obj.pressedButton then
                        event.push("drag", screen, obj.posX, obj.posY, obj.pressedButton, nikname)
                    end
                end
                if eventName == "key_down" or eventName == "key_up" then
                    if not obj.pressedButton or eventName ~= "key_down" then
                        if code == keyboardLib.keys.enter then
                            if eventName == "key_down" then
                                event.push("touch", screen, obj.posX, obj.posY, 0, nikname)
                                obj.pressedButton = 0
                            elseif eventName == "key_up" then
                                event.push("drop", screen, obj.posX, obj.posY, 0, nikname)
                                obj.pressedButton = false
                            end
                        elseif code == keyboardLib.keys.tab then
                            if eventName == "key_down" then
                                event.push("touch", screen, obj.posX, obj.posY, 1, nikname)
                                obj.pressedButton = 1
                            elseif eventName == "key_up" then
                                event.push("drop", screen, obj.posX, obj.posY, 1, nikname)
                                obj.pressedButton = false
                            end
                        end
                    end
                end
            end
        end
    end)

    return obj
end

-------------------------------------------

local th

function start()
    if th then return end
    th = createCursor().thread
    th:detach()
    _G.rcCursor = true
end

function stop()
    if not th then return end
    th:kill()
    th = nil
    _G.rcCursor = nil
end