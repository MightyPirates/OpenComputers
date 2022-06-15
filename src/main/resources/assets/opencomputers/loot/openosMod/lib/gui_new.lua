local component = require("component")
local event = require("event")
local term = require("term")
local unicode = require("unicode")
local thread = require("thread")
local computer = require("computer")
local keyboard = require("keyboard")

--------------------------------------------

local keys = keyboard.keys

local function map(value, low, high, low_2, high_2)
    local relative_value = (value - low) / (high - low)
    local scaled_value = low_2 + (high_2 - low_2) * relative_value
    return scaled_value
end

local function startDrawer(code, gpu, posX, posY, sizeX, sizeY, state)
    local oldb = gpu.getBackground()
    local oldf = gpu.getForeground()
    code(gpu, posX, posY, sizeX, sizeY, state)
    gpu.setBackground(oldb)
    gpu.setForeground(oldf)
end

local function drawtext(gpu, color, posX, posY, text, simple)
    local oldb = gpu.getBackground()
    local oldf = gpu.setForeground(color)
    local rx, ry = gpu.getResolution()
    if not simple then
        for i = 1, unicode.len(text) do
            local char = unicode.sub(text, i, i)
            if posX + (i - 1) < 1 or posX + (i - 1) > rx then
                break
            end
            local _, _, nb = gpu.get(posX + (i - 1), posY)
            gpu.setBackground(nb)
            gpu.set(posX + (i - 1), posY, char)
        end
    else
        gpu.setForeground(color)
        local _, _, back = gpu.get(posX, posY)
        gpu.setBackground(back)
        gpu.set(posX, posY, text)
    end
    gpu.setBackground(oldb)
    gpu.setForeground(oldf)
end

local function getClick(posX, posY, sizeX, sizeY, touchX, touchY)
    if touchX >= posX and touchX < (posX + sizeX) then
        if touchY >= posY and touchY < (posY + sizeY) then
            return true
        end
    end
end

local function getStandertOffColors(gpu)
    local depth = math.floor(gpu.getDepth())
    if depth == 1 then
        return 0x000000, 0xFFFFFF
    else
        return 0x666666, 0x444444
    end
end

local function touchIn(screen, posX, posY, button)
    event.push("touch", screen, posX, posY, button or 0)
end

local function blinkIn(gpu, posX, posY)
    local oldb = gpu.getBackground()
    local oldf = gpu.getForeground()
    local char, fore, back = gpu.get(posX, posY)

    gpu.setBackground(0xFFFFFF - back)
    gpu.setForeground(0xFFFFFF - fore)
    gpu.set(posX, posY, char)

    gpu.setBackground(oldb)
    gpu.setForeground(oldf)
end

local function getCenter(maxpos, size)
    return (math.floor(maxpos / 2) - math.floor(size / 2)) + 1
end

local function orValue(value, standart)
    if value == nil then return standart end
    return value
end

--------------------------------------------

return {create = function(customX, customY)
    local lib = {}
    lib.gpu = term.gpu()
    lib.scenes = {}
    lib.selected = 0
    lib.screen = term.screen()
    lib.keyboard = term.keyboard()
    lib.closeallow = true
    lib.colorfilter = true
    lib.exitcallbacks = {}

    local function getColor(gpu, color)
        if not color then return nil end
        if not lib.colorfilter then return color end
        local depth = math.floor(gpu.getDepth())
        if depth == 4 then
            if color == 0x00FFFF then
                return 0x00AAFF
            end
            return color
        elseif depth == 1 then
            return nil
        else
            return color
        end
    end

    lib.filter = function(color) 
        checkArg(1, color, "number")
        return getColor(lib.gpu, color)
    end

    if not lib.gpu or not lib.screen then
        error("this lib(gui_new) required gpu and screen to work")
    end
    if component.proxy(lib.screen).isPrecise() then
        error("this lib(gui_new) do not supported precise mode")
    end

    local maxX, maxY = lib.gpu.maxResolution()
    local oldrx, oldry = lib.gpu.getResolution()
    local rx, ry = customX or maxX, customY or maxY
    lib.rx = rx
    lib.ry = ry
    lib.exit = function(bool)
        if not lib.closeallow and bool then return end
        for i = 1, #lib.exitcallbacks do lib.exitcallbacks[i]() end
        lib.gpu.setResolution(oldrx, oldry)
        lib.gpu.setBackground(0)
        lib.gpu.setForeground(0xFFFFFF)
        term.clear()
        os.exit()
    end
    require("process").info().data.signal = function() lib.exit(true) end

    ------------------------------------------------

    lib.getScene = function(num)
        local number = num or lib.selected
        return lib.scenes[number]
    end

    lib.resetButtons = function()
        local objs = lib.getScene().objs
        for i = 1, #objs do
            local obj = objs[i]
            if obj.togle == false and obj.state ~= nil then
                obj.state = false
            end
        end
    end

    lib.interrupt = function(...)
        if lib.cursor then
            lib.cursor.insertEvent(...)
        end
    end

    lib.uploadEvent = function(...)
        lib.interrupt(...)
        if lib.selected == 0 then return end
        lib.resetButtons()
        local scene = lib.getScene()
        local elements = scene.objs
        for i2 = 1, #elements do
            local obj = elements[i2]
            if obj.insertEvent then
                obj.insertEvent(...)
            end
        end
    end

    lib.uploadEventPro = function(data, tab)
        lib.interrupt(table.unpack(data))
        if lib.selected == 0 then return end
        lib.resetButtons()
        local scene = lib.getScene()
        local elements = tab or scene.objs
        for i2 = 1, #elements do
            local obj = elements[i2]
            if obj.insertEvent then
                obj.insertEvent(table.unpack(data))
            end
        end
    end

    lib.redraw = function()
        if lib.selected == 0 then return end
        if lib.cursor then
            lib.cursor.num = 0
        end
        lib.getScene().draw()
    end

    lib.select = function(num)
        checkArg(1, num, "number", "table")
        if lib.cursor then
            lib.cursor.posX = 1
            lib.cursor.posY = 1
        end
        if type(num) ~= "number" then
            for i = 1, #lib.scenes do
                if lib.scenes[i] == num then
                    lib.selected = i
                    lib.redraw()
                    break
                end
            end
        else
            lib.selected = num
            if num == 0 then
                lib.gpu.setResolution(lib.rx, lib.ry)
                term.clear()
            else
                lib.redraw()
            end
        end
    end

    lib.createExitButtons = function(posX, posY)
        for i = 1, #lib.scenes do
            lib.getScene(i).createExitButton()
        end
    end

    lib.delete = function(num)
        checkArg(1, num, "number", "table", "nil")
        if type(num) == "table" then
            for i = 1, #lib.scenes do
                if lib.scenes[i] == num then
                    lib.scenes[i] = nil
                    break
                end
            end
        elseif type(num) == "number" then
            lib.scenes[num] = nil
        elseif type(num) == "nil" then
            lib.scenes[lib.selected] = nil
        end
    end

    ------------------------------------------------

    lib.createCursor = function()
        local obj = {}
        obj.posX = 1
        obj.posY = 1
        blinkIn(lib.gpu, obj.posX, obj.posY)

        obj.draw = function()
        end

        local oldchar, oldfore, oldback
        obj.insertEvent = function(...)
            local eventName, keyboard, char, code = ...
            if eventName ~= "key_down" or keyboard ~= lib.keyboard then
                return
            end
            local rx, ry = lib.gpu.getResolution()
            if code == keys.enter then
                touchIn(lib.screen, obj.posX, obj.posY)
                return
            end
            if code == keys.tab then
                touchIn(lib.screen, obj.posX, obj.posY, 1)
                return
            end
            local tx, ty = obj.posX, obj.posY
            if code == keys.up then
                ty = ty - 1
            elseif code == keys.down then
                ty = ty + 1
            elseif code == keys.left then
                tx = tx - 1
            elseif code == keys.right then
                tx = tx + 1
            else
                return
            end
            if ty < 1 then ty = 1 end
            if ty > ry then ty = ry end
            if tx < 1 then tx = 1 end
            if tx > rx then tx = rx end

            if oldchar and oldfore and oldback then
                local char, fore, back = lib.gpu.get(obj.posX, obj.posY)
                if oldchar ~= char or oldfore ~= fore or oldback ~= back then
                    blinkIn(lib.gpu, obj.posX, obj.posY)
                end
            end

            blinkIn(lib.gpu, obj.posX, obj.posY)
            obj.posX = tx
            obj.posY = ty
            blinkIn(lib.gpu, obj.posX, obj.posY)
            oldchar, oldfore, oldback = lib.gpu.get(obj.posX, obj.posY)
        end
        
        lib.cursor = obj
        return obj
    end
    if math.floor(computer.getDeviceInfo()[lib.screen].width) == 1 then
        lib.createCursor()
    end

    lib.useCursor = function(state)
        if state then
            if not lib.cursor then
                lib.cursor = lib.createCursor()
            end
        else
            lib.cursor = nil
        end
    end

    lib.createScene = function(color, rx, ry)
        local scene = {}
        scene.color = getColor(lib.gpu, color) or 0
        scene.objs = {}
        scene.rx = rx or lib.rx
        scene.ry = ry or lib.ry

        scene.draw = function()
            local rx, ry = scene.rx, scene.ry
            lib.gpu.setResolution(rx, ry)
            if type(scene.color) == "number" then
                local oldb = lib.gpu.setBackground(scene.color)
                lib.gpu.fill(1, 1, rx, ry, " ")
                lib.gpu.setBackground(oldb)
            else
                startDrawer(scene.color, lib.gpu, 1, 1, rx, ry)
            end
            for i = 1, #scene.objs do
                scene.objs[i].draw()
            end
        end

        scene.detele = function()
            lib.delete(scene)
        end

        scene.createButton = function(posX, posY, sizeX, sizeY, text, back, fore, togle, state, back2, fore2, callback, relased)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, sizeX)
            obj.posY = posY or getCenter(scene.ry, sizeY)
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.text = text or " "
            obj.back = getColor(lib.gpu, back) or 0xFFFFFF
            obj.fore = getColor(lib.gpu, fore) or 0x000000
            obj.togle = togle or false
            obj.state = state or false
            local standert1, standert2 = getStandertOffColors(lib.gpu)
            obj.back2 = getColor(lib.gpu, back2) or standert1
            obj.fore2 = getColor(lib.gpu, fore2) or standert2
            obj.relased = relased
            obj.callbacks = {callback}

            obj.draw = function()
                if lib.getScene() ~= scene then return end
                local function text(color, simple)
                    drawtext(lib.gpu, color, (obj.posX + math.floor(obj.sizeX / 2)) - math.floor(unicode.len(obj.text) / 2), obj.posY + math.floor(obj.sizeY / 2), obj.text, simple)
                end
                if obj.togle then
                    if type(obj.back) == "number" then
                        local oldb
                        if obj.state then
                            oldb = lib.gpu.setBackground(obj.back)
                        else
                            oldb = lib.gpu.setBackground(obj.back2)
                        end
                        lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                        lib.gpu.setBackground(oldb)
                        if obj.state then
                            text(obj.fore, true)
                        else
                            text(obj.fore2, true)
                        end
                    else
                        startDrawer(obj.back, lib.gpu, obj.posX, obj.posY, obj.sizeX, obj.sizeY, obj.state)
                        if obj.state then
                            text(obj.fore)
                        else
                            text(obj.fore2)
                        end
                    end
                else
                    if type(obj.back) == "number" then
                        local oldb = lib.gpu.setBackground(obj.back)
                        lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                        lib.gpu.setBackground(oldb)
                        text(obj.fore, true)
                    else
                        startDrawer(obj.back, lib.gpu, obj.posX, obj.posY, obj.sizeX, obj.sizeY, true)
                        text(obj.fore)
                    end
                end
            end

            obj.insertEvent = function(...)
                local eventName, uuid, touchX, touchY, button = ...
                if ((not obj.relased and eventName ~= "touch") or (obj.relased and eventName ~= "drop")) or uuid ~= lib.screen or button ~= 0 then
                    return
                end
                local click = getClick(obj.posX, obj.posY, obj.sizeX, obj.sizeY, touchX, touchY)
                if not click then
                    return
                end
                if obj.togle then
                    obj.state = not obj.state
                    obj.draw()
                    for i = 1, #obj.callbacks do
                        obj.callbacks[i](obj.state)
                    end
                else
                    obj.state = true
                    for i = 1, #obj.callbacks do
                        obj.callbacks[i]()
                    end
                end
            end

            obj.getState = function()
                local out = obj.state
                if not obj.togle then
                    obj.state = false
                end
                return out
            end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        scene.createLabel = function(posX, posY, sizeX, sizeY, text, back, fore)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, sizeX)
            obj.posY = posY or getCenter(scene.ry, sizeY)
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.text = text or " "
            obj.back = getColor(lib.gpu, back) or 0xFFFFFF
            obj.fore = getColor(lib.gpu, fore) or 0x000000
    
            obj.draw = function()
                if lib.getScene() ~= scene then return end
                local function text(color, simple)
                    drawtext(lib.gpu, color, (obj.posX + math.floor(obj.sizeX / 2)) - math.floor(unicode.len(obj.text) / 2), obj.posY + math.floor(obj.sizeY / 2), obj.text, simple)
                end
                if type(obj.back) == "number" then
                    local oldb = lib.gpu.setBackground(obj.back)
                    lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                    lib.gpu.setBackground(oldb)
                    text(obj.fore, true)
                else
                    startDrawer(obj.back, lib.gpu, obj.posX, obj.posY, obj.sizeX, obj.sizeY, true)
                    text(obj.fore)
                end
            end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        scene.createExitButton = function(posX, posY)
            scene.createButton(posX or scene.rx, posY or 1, 1, 1, "X", 0xFF0000, 0xFFFFFF, nil, nil, nil, nil, function() lib.exit() end)
        end

        scene.createSeekBar = function(posX, posY, sizeX, back, fore, min, max, value, touch, mode, callback, autolabel, text, labelsize)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, (mode == 1 and 1) or sizeX)
            obj.posY = posY or getCenter(scene.ry, (mode == 1 and sizeX) or 1)
            obj.sizeX = sizeX
            if autolabel then
                if not labelsize then
                    labelsize = math.floor(obj.sizeX / 8)
                end
                obj.sizeX = sizeX - labelsize
            end
            obj.back = getColor(lib.gpu, back) or 0xFFFFFF
            obj.fore = getColor(lib.gpu, fore) or 0x000000
            obj.min = min or 0
            obj.max = max or 1
            obj.touch = orValue(touch, true)
            obj.mode = mode or 0
            obj.callbacks = {callback}
            obj.value = math.floor(map(value or obj.min or 0, obj.min, obj.max, 0, obj.sizeX - 1))

            if autolabel then
                local label = scene.createLabel(obj.posX + obj.sizeX, obj.posY, labelsize, 1, ((text and text..":") or "")..tostring(math.floor(value / 0.1) * 0.1), obj.back, obj.fore)
                obj.callbacks[#obj.callbacks + 1] = function(value)
                    label.text = ((text and text..":") or "")..tostring(math.floor(value / 0.1) * 0.1)
                    label.draw()
                end
                obj.label = label
            end

            obj.draw = function()
                if lib.getScene() ~= scene then return end
                local oldb = lib.gpu.setBackground(obj.back)
                local oldf = lib.gpu.setForeground(obj.fore)

                if obj.mode == 0 then
                    lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, 1, " ")
                    lib.gpu.set(obj.posX + obj.value, obj.posY, "#")
                elseif obj.mode == 1 or obj.mode == 2 then
                    lib.gpu.fill(obj.posX, obj.posY, 1, obj.sizeX, " ")
                    if obj.mode == 1 then
                        lib.gpu.set(obj.posX, (obj.posY + (obj.sizeX - 1)) - obj.value, "#")
                    else
                        lib.gpu.set(obj.posX, obj.posY + obj.value, "#")
                    end
                end

                lib.gpu.setBackground(oldb)
                lib.gpu.setForeground(oldf)
            end

            obj.getState = function()
                return map(obj.value, 0, obj.sizeX - 1, obj.min, obj.max)
            end

            obj.setState = function(new)
                local old = obj.getState()
                obj.value = math.floor(map(new, obj.min, obj.max, 0, obj.sizeX - 1))
                obj.draw()
                return old
            end

            obj.getValue = function()
                return obj.value
            end

            obj.setValue = function(new)
                if new < 0 or new > obj.sizeX - 1 then
                    return nil
                end
                local old = obj.value
                obj.value = new
                obj.draw()
                return old
            end

            obj.insertEvent = function(...)
                local eventName, uuid, touchX, touchY, button = ...
                if (eventName ~= "touch" and eventName ~= "drag") or uuid ~= lib.screen or button ~= 0 or not obj.touch then
                    return
                end
                local oldv = obj.getState()
                if (touchY == obj.posY and obj.mode == 0) or (touchX == obj.posX and (obj.mode == 1 or obj.mode == 2)) then
                    if (touchX >= obj.posX and touchX < (obj.posX + obj.sizeX) and obj.mode == 0) or (touchY >= obj.posY and touchY < (obj.posY + obj.sizeX) and (obj.mode == 1 or obj.mode == 2)) then
                        local value
                        if obj.mode == 0 then
                            value = (touchX - (obj.posX - 1)) - 1
                        elseif obj.mode == 1 or obj.mode == 2 then
                            if obj.mode == 1 then
                                value = obj.sizeX - (touchY - (obj.posY - 1))
                            else
                                value = touchY - obj.posY
                            end
                        end
                        obj.value = value
                        obj.draw()
                        for i = 1, #obj.callbacks do
                            obj.callbacks[i](obj.getState(), oldv)
                        end
                    end
                end
            end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        scene.createLogZone = function(posX, posY, sizeX, sizeY, back, fore, seek_back, seek_fore, autoscroll)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, sizeX)
            obj.posY = posY or getCenter(scene.ry, sizeY)
            obj.sizeX = sizeX - 1
            obj.sizeY = sizeY
            obj.back = getColor(lib.gpu, back) or 0xFFFFFF
            obj.fore = getColor(lib.gpu, fore) or 0x000000
            obj.seek_back = getColor(lib.gpu, seek_back) or 0xFFFFFF
            obj.seek_fore = getColor(lib.gpu, seek_fore) or 0x000000
            obj.seekvalue = 0
            obj.datalist = {}
            obj.maxstrs = obj.sizeY * (obj.sizeY / 2)
            obj.autoscroll = orValue(autoscroll, true)
            obj.autodraw = true
            local function drawData()
                obj.draw()
            end
            obj.seekbar = scene.createSeekBar((posX + sizeX) - 1, posY, sizeY, seek_back, seek_fore, 1, #obj.datalist, 1, true, 2, drawData)
            obj.strs = {}

            obj.draw = function()
                if lib.getScene() ~= scene then return end
                obj.strs = {}
                local oldb = lib.gpu.setBackground(obj.back)
                local oldf = lib.gpu.setForeground(obj.fore)
                lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                obj.seekbar.draw()
                for i = 1, #obj.datalist do
                    local select = (math.floor(obj.seekbar.getState()) + 1) - i
                    if select > 0 and select <= #obj.datalist then
                        local data = obj.datalist[select]
                        local posY = (obj.posY + obj.sizeY) - i
                        if posY >= obj.posY then
                            lib.gpu.set(obj.posX, posY, data)
                            obj.strs[posY] = data
                        else
                            return
                        end
                    else
                        return
                    end
                end
                lib.gpu.setBackground(oldb)
                lib.gpu.setForeground(oldf)
            end

            obj.clearOld = function()
                for i = 1, #obj.datalist do
                    obj.datalist[i] = obj.datalist[i + 1]
                end
            end

            obj.clear = function()
                obj.datalist = {}
                obj.seekbar.max = #obj.datalist
                obj.seekbar.setValue(sizeY)
            end

            obj.add = function(str)
                obj.datalist[#obj.datalist + 1] = str
                while #obj.datalist > obj.maxstrs do
                    obj.clearOld()
                end
                obj.seekbar.max = #obj.datalist
                if obj.autoscroll then obj.seekbar.setValue(obj.sizeY - 1) end
                if obj.autodraw then obj.draw() end
            end

            obj.setpos = function(num)
                obj.seekbar.setState(num)
            end

            obj.insertEvent = function(...)
                local eventName, uuid, touchX, touchY, value = ...
                obj.touchInfo = nil
                if (eventName == "touch" or eventName == "drag" or eventName == "drop") and uuid == lib.screen then
                    obj.touchInfo = {...}
                end
                if eventName ~= "scroll" or uuid ~= lib.screen then
                    return
                end
                if touchX >= obj.posX and touchX < (obj.posX + obj.sizeX) + 1 then
                    if touchY >= obj.posY and touchY < (obj.posY + obj.sizeY) then
                        obj.seekbar.setValue(obj.seekbar.getValue() - value)
                        obj.draw()
                    end
                end
            end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        scene.createInputBox = function(posX, posY, sizeX, sizeY, text, back, fore, callback)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, sizeX)
            obj.posY = posY or getCenter(scene.ry, sizeY)
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.text = text
            obj.back = getColor(lib.gpu, back) or 0xFFFFFF
            obj.fore = getColor(lib.gpu, fore) or 0x000000
            obj.value = ""
            obj.wail = false
            obj.callbacks = {callback}
            local function input()
                if obj.wail then return end
                obj.wail = true
                local text = obj.button.text
                obj.button.text = ""
                obj.button.draw()

                local function read()
                    term.setCursor(obj.posX, obj.posY + math.floor(obj.sizeY / 2))

                    local out = io.read()
                    obj.value = out

                    obj.button.text = text
                    obj.button.draw()

                    lib.redraw()

                    obj.wail = false

                    for i = 1, #obj.callbacks do
                        obj.callbacks[i](obj.value)
                    end
                end
                read()
            end

            obj.button = scene.createButton(obj.posX, obj.posY, obj.sizeX, obj.sizeY, obj.text, obj.back, obj.fore, nil, nil, nil, nil, input)

            obj.get = function() return obj.value end
            obj.input = input
            obj.draw = function() end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        scene.createDrawZone = function(posX, posY, sizeX, sizeY, image, index)
            local obj = {}
            obj.posX = posX or getCenter(scene.rx, sizeX)
            obj.posY = posY or getCenter(scene.ry, sizeY)
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.image = image
            obj.index = index or 0
    
            obj.draw = function()
                if lib.getScene() ~= scene then return end
                startDrawer(obj.image, lib.gpu, obj.posX, obj.posY, obj.sizeX, obj.sizeY, true)
            end

            obj.insertEvent = function(...)
                local eventName, uuid, touchX, touchY, button = ...
                if (eventName ~= "touch" and eventName ~= "drag" and eventName ~= "drop") or uuid ~= lib.screen then
                    return
                end
                local lx, ly = 0, 0
                if touchX >= obj.posX and touchX < obj.posX + obj.sizeX then
                    if touchY >= obj.posY and touchY < obj.posY + obj.sizeY then
                        lx = (touchX - obj.posX) + 1
                        ly = (touchY - obj.posY) + 1
                        event.push("drawZone", eventName, obj.index, lx, ly, button)
                    end
                end
            end

            scene.objs[#scene.objs + 1] = obj
            return obj
        end

        lib.scenes[#lib.scenes + 1] = scene
        return scene
    end

    lib.context = function(skip, posX, posY, datain, duplicateEvent)
        local data = {}
        if type(datain[1]) ~= "table" then
            for i = 1, #datain do
                data[i] = {datain[i], true}
            end
        else
            data = datain
        end
        local texts = {}
        local activates = {}
        
        local sizeX = 0
        local sizeY = #data
    
        for i = 1, #data do
            local dat = data[i]
            texts[i] = dat[1]
            activates[i] = dat[2]
            if unicode.len(dat[1]) > sizeX then
                sizeX = unicode.len(dat[1])
            end
        end
    
        local gpu = lib.gpu
        local depth = lib.gpu.getDepth()
        local rx, ry = lib.gpu.getResolution()
        if posY + sizeY > (ry + 1) then posY = posY - (sizeY - 1) end
        if posX + sizeX > (rx + 1) then posX = posX - (sizeX - 1) end
    
        local oldb = gpu.getBackground()
        local oldf = gpu.getForeground()
        
        if math.floor(gpu.getDepth()) ~= 1 then
            gpu.setBackground(0x444444)
            gpu.fill(posX + 1, posY + 1, sizeX, sizeY, " ")
        else
            gpu.setBackground(0x000000)
            gpu.setForeground(0xFFFFFF)
            gpu.fill(posX + 1, posY + 1, sizeX, sizeY, "#")
        end
    
        gpu.setBackground(0xFFFFFF)
        gpu.fill(posX, posY, sizeX, sizeY, " ")
    
        for i = 1, sizeY do
            local text = texts[i]
            local activate = activates[i]
            if math.floor(gpu.getDepth()) ~= 1 then
                if activate then
                    gpu.setForeground(0x000000)
                else
                    gpu.setForeground(0x666666)
                end
            else
                if activate then
                    gpu.setForeground(0x000000)
                    gpu.setBackground(0xFFFFFF)
                else
                    gpu.setForeground(0xFFFFFF)
                    gpu.setBackground(0x000000)
                end
            end
            gpu.set(posX, posY + (i - 1), text)
        end
    
        local out
        while true do
            lib.count = 0
            local tab = {event.pull(0.3)}
            local eventName, uuid, x, y, button = table.unpack(tab or {})
            lib.interrupt(table.unpack(tab or {}))
            if eventName == "touch" and uuid == lib.screen then
                if button ~= 0 and skip then
                    if duplicateEvent then event.push(table.unpack(tab)) end
                    break
                end
                if button == 0 then
                    if x >= posX and x <= ((posX + sizeX) - 1) then
                        local index = y - (posY - 1)
                        if index < 1 or index > #texts then
                            if skip then
                                if duplicateEvent then event.push(table.unpack(tab)) end
                                break
                            end
                        else
                            if activates[index] then
                                out = index
                                break
                            end
                        end
                    elseif skip then
                        if duplicateEvent then event.push(table.unpack(tab)) end
                        break
                    end
                end
            end
        end

        lib.redraw()
    
        gpu.setBackground(oldb)
        gpu.setForeground(oldf)
    
        return texts[out], out
    end

    function lib.yesno(text)
        local gpu = lib.gpu
        lib.gpu.setResolution(lib.rx, lib.ry)
        local depth = gpu.getDepth()
        local rx, ry = gpu.getResolution()
    
        local color1 = 0xFF0000
        local color2 = 0x00FF00
    
        if math.floor(depth) == 1 then
            color1 = 0x0
            color2 = 0x0
        end
    
        local gui = lib.createScene(0xFFFFFF)
        gui.createLabel(1, 1, rx, 1, text, 0, 0xFFFFFF)
        local yes = gui.createButton(nil, nil, rx, 3, "yes", color2, 0xffffff)
        local no = gui.createButton(nil, nil, rx, 3, "no", color1, 0xffffff) yes.posY = yes.posY + 3
        
        local oldselect = lib.selected
        lib.select(gui)
    
        local out
        while true do
            lib.count = 0
            local eventData = {event.pull(0.3)}
            lib.interrupt(table.unpack(eventData or {}))
            if eventData[1] == "touch" and eventData[2] == lib.screen and eventData[5] == 0 then
                lib.uploadEventPro(eventData, {yes, no})
                if yes.getState() then
                    out = true
                    break
                elseif no.getState() then
                    out = false
                    break
                end
            end
        end

        lib.scenes[lib.selected] = nil
        lib.select(oldselect or 0)
    
        return out
    end
    
    lib.splas = function(text)
        local gpu = lib.gpu
        gpu.setResolution(lib.rx, lib.ry)
        local depth = gpu.getDepth()
        local rx, ry = gpu.getResolution()
    
        local color = 0x0000FF
        if math.floor(depth) == 1 then
            color = 0x0
        end
    
        local gui = lib.createScene(0xFFFFFF)
        gui.createLabel(1, 1, rx, 1, text, 0, 0xFFFFFF)
        local ok = gui.createButton(nil, nil, rx, 3, "ok", color, 0xffffff)

        local oldselect = lib.selected
        lib.select(gui)
    
        while true do
            lib.count = 0
            local eventData = {event.pull(0.3)}
            lib.interrupt(table.unpack(eventData or {}))
            if eventData[1] == "touch" and eventData[2] == lib.screen and eventData[5] == 0 then
                lib.uploadEventPro(eventData, {ok})
                if ok.getState() then
                    break
                end
            end
        end

        lib.scenes[lib.selected] = nil
        lib.select(oldselect or 0)
    end
    lib.splash = lib.splas

    lib.menu = function(label, strs, num)
        local oldScene = lib.getScene()
        lib.select(0)
        local gpu = lib.gpu
        local rx, ry = gpu.getResolution()
        local oldf = gpu.setForeground(0)
        local oldb = gpu.setBackground(0xFFFFFF)

        local function invert() gpu.setForeground(gpu.setBackground(gpu.getForeground())) end
        local function setText(text, posY) 
            gpu.set(math.ceil((rx / 2) - (unicode.len(text) / 2)), posY, text)
        end

        local out
        local select = num or 1
        while true do
            term.clear()
            local startpos = math.floor(select / ry) * ry
            if startpos == 0 then
                invert()
                setText(label, 1)
                invert()
            end
            for i = 1, #strs do
                local pos = (i + 1) - startpos
                if pos >= 1 and pos <= ry then
                    if term.keyboard() and select == i then invert() end
                    setText(strs[i], pos)
                    if term.keyboard() and select == i then invert() end
                end
            end
            local eventName, uuid, _, code, button = event.pull()
            if eventName == "key_down" and uuid == term.keyboard() then
                if code == 200 and select > 1 then
                    select = select - 1
                end
                if code == 208 and select < #strs then
                    select = select + 1
                end
                if code == 28 then
                    out = select
                    break
                end
            elseif eventName == "touch" and uuid == term.screen() and button == 0 then
                code = code + startpos
                code = code - 1
                if code >= 1 and code <= #strs then
                    out = code
                    break
                end
            elseif eventName == "scroll" and uuid == term.screen() then
                if button == 1 and select > 1 then
                    select = select - 1
                end
                if button == -1 and select < #strs then
                    select = select + 1
                end
            end
        end

        gpu.setBackground(oldb)
        gpu.setForeground(oldf)
        lib.select(oldScene or 0)
        return strs[out], out
    end
    
    function lib.status(text)
        local gpu = lib.gpu
        local depth = math.floor(gpu.getDepth())
        local rx, ry = gpu.getResolution()
        local oldf = gpu.setForeground(0)
        local oldb = gpu.setBackground(0xFFFFFF)

        local x, y = math.ceil((rx / 2) - (unicode.len(text) / 2)), ry // 2
        gpu.fill(x - 1, y - 1, unicode.len(text) + 2, 3, "╳")
        gpu.set(x, y, text)

        gpu.setBackground(oldb)
        gpu.setForeground(oldf)
    end

    return lib
end}