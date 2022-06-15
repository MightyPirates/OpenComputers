local unicode = require("unicode")
local term = require("term")
local process = require("process")
local event = require("event")
local su = require("superUtiles")
local thread = require("thread")
local component = require("component")
local computer = require("computer")
local keyboard = require("keyboard")
local colorPic = require("colorPic")

-----------------------------------------

local function orValue(value, standart)
    if value == nil then return standart end
    return value
end

local function runCallback(callback, ...)
    if not callback then return true end
    return xpcall(callback, function(str) event.onError(str) end, ...)
end

local function table_remove(tbl, obj)
    for i = 1, #tbl do
        if tbl[i] == obj then
            table.remove(tbl, i)
        end
    end
end

-----------------------------------------

return {create = function(minTier)
    local lib = {}

    if not term.isAvailable() then
        error("term is not available", 0)
    end

    --link
    lib.gpu = term.gpu()
    lib.screen = term.screen()
    lib.keyboard = term.keyboard()
    lib.keyboards = term.keyboards()

    local maxDepth = math.floor(lib.gpu.maxDepth())
    if maxDepth == 8 then
        lib.tier = 3
    elseif maxDepth == 4 then
        lib.tier = 2
    elseif maxDepth == 1 then
        lib.tier = 1
    else
        lib.tier = math.huge
    end

    if minTier and lib.tier < minTier then
        io.stderr:write("programm required tier " .. tostring(math.floor(minTier)) .. ", you have tier " .. tostring(math.floor(lib.tier)))
        os.exit()
    end

    --check params
    local _, oldPreciseState = pcall(component.invoke, lib.screen, "isPrecise") --pcall на всякий случай
    pcall(component.invoke, lib.screen, "setPrecise", false) --precise больше не поддерживаться
    local _, preciseState = pcall(component.invoke, lib.screen, "isPrecise")

    lib.noTouch = math.floor(tonumber(computer.getDeviceInfo()[lib.screen].width)) == 1

    --[[
    if lib.noTouch then
        io.stderr:write("error to open gui, screen in not touchable and 'rc cursor' is not on, run program in clickable screen or run 'rc cursor start'")
        os.exit()
    end
    ]]

    --params
    lib.active = true
    lib.soundOn = true
    lib.block = false
    lib.redrawAll = false
    lib.startTime = computer.uptime() --кастыль для исправления паразитных нажатий кнопок при переключении сцен

    --gpu params
    lib.userX, lib.userY = lib.gpu.getResolution()
    lib.maxX, lib.maxY = lib.gpu.maxResolution()
    lib.depth = math.floor(lib.gpu.getDepth())
    lib.reset_gpu = su.saveGpu(lib.gpu.address)

    lib.colors = colorPic.getColors()
    local colors = lib.colors --да говнокод знаю
    lib.gpu.setBackground(colors.black)
    lib.gpu.setForeground(colors.white)
    term.clear()

    --functions
    local function isZone(obj, touchX, touchY)
        if preciseState then
            touchX = touchX + 1
            touchY = touchY + 1
        end
        return touchX >= obj.posX and touchX < (obj.posX + obj.sizeX) and touchY >= obj.posY and touchY < (obj.posY + obj.sizeY)
    end
    local function optimizeBeep(n ,d)
        if component.isAvailable("beep") then
            component.beep.beep({[n] = d})
        else
            computer.beep(n, d)
        end
    end
    local function soundNum(num)
        if lib.soundOn then
            if num == 0 then
                optimizeBeep(2000, 0.01)
            elseif num == 1 then
                optimizeBeep(400, 0.01)
            elseif num == 2 then
                optimizeBeep(40, 0.01)
            end
        end
    end

    function lib.selectColor(mainColor, miniColor, bw)
        local depth = lib.depth
        if type(bw) == "boolean" then bw = bw and colors.white or colors.black end

        if depth == 4 then
            return miniColor or mainColor
        elseif depth == 1 then
            return bw or mainColor
        end
        return mainColor
    end

    local function createThreadsMenager(scene)
        local mainObj = {}
        mainObj.timers = {}
        mainObj.listens = {}
        mainObj.threads = {}

        function mainObj.createTimer(time, callback, times)
            local obj = {}
            obj.on = not scene or lib.scene == scene
            obj.times = times or math.huge
            obj.id = event.timer(time, function(...)
                obj.times = obj.times - 1
                if not obj.on or lib.startTime > computer.uptime() then return end

                local killed = false
                if obj.times <= 0 then obj.kill() killed = true end --досрочьный kill вдруг функйия будет блокируюшия

                local stopState = callback(...)

                if stopState == false or obj.times <= 0 then
                    if not killed then obj.kill() end
                    return false
                end
            end, math.huge) --и да я сам реализовываю каунтер
            function obj.kill()
                event.cancel(obj.id)
                obj.on = false
                table_remove(mainObj.timers, obj)
            end

            table.insert(mainObj.timers, obj)
            return obj
        end

        function mainObj.createListen(eventType, callback)
            local obj = {}
            obj.on = not scene or lib.scene == scene
            obj.id = event.register(eventType, function(inputEventType, ...)
                if not obj.on or (eventType and inputEventType ~= eventType) or not inputEventType or lib.startTime > computer.uptime() then return end
                local stopState = callback(inputEventType, ...)

                if stopState == false then
                    obj.kill()
                    return false
                end
            end, math.huge, math.huge)
            function obj.kill()
                event.cancel(obj.id)
                obj.on = false
                table_remove(mainObj.listens, obj)
            end

            table.insert(mainObj.listens, obj)
            return obj
        end

        function mainObj.createThread(func, ...)
            local obj = {}
            obj.thread = thread.create(func, ...)
            if scene and lib.scene ~= scene then obj.thread:suspend() end

            function obj.kill()
                obj.thread:kill()
                table_remove(mainObj.threads, obj)
            end

            table.insert(mainObj.threads, obj)
            return obj
        end

        function mainObj.killAll()
            for i, data in ipairs(mainObj.timers) do
                event.cancel(data.id)
                mainObj.timers[i] = nil
            end
            for i, data in ipairs(mainObj.listens) do
                event.cancel(data.id)
                mainObj.listens[i] = nil
            end
            for i, data in ipairs(mainObj.threads) do
                data.thread:kill()
                mainObj.threads[i] = nil
            end
        end

        function mainObj.stopAll()
            for i, data in ipairs(mainObj.timers) do
                data.on = false
            end
            for i, data in ipairs(mainObj.listens) do
                data.on = false
            end
            for i, data in ipairs(mainObj.threads) do
                data.thread:suspend()
            end
        end

        function mainObj.startAll()
            for i, data in ipairs(mainObj.timers) do
                data.on = true
            end
            for i, data in ipairs(mainObj.listens) do
                data.on = true
            end
            for i, data in ipairs(mainObj.threads) do
                data.thread:resume()
            end
        end

        return mainObj
    end

    local function createCursor()
        local obj = {}
        obj.posX = 1
        obj.posY = 1
        obj.pressedButton = false
    
        obj.blickState = false
        function obj.draw()
            local char, fore, back = lib.gpu.get(obj.posX, obj.posY)
            lib.gpu.setBackground(0xFFFFFF - back)
            lib.gpu.setForeground(0xFFFFFF - fore)
            lib.gpu.set(obj.posX, obj.posY, char)
            obj.blickState = not obj.blickState
        end

        function obj.setBlick(state)
            if obj.blickState ~= state then
                obj.draw()
            end
        end

        function obj.posCheck()
            local rx, ry = lib.gpu.getResolution()
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

        obj.thread = lib.createThread(function()
            while true do
                obj.draw()
                ::tonew::
                local eventName, uuid, char, code, nikname = event.pull(0.5)
                if not su.inTable(lib.keyboards, uuid) then goto tonew end
                obj.posCheck()

                if keyboard.isControlDown() then
                    if eventName == "key_down" then
                        if code == keyboard.keys.up then
                            event.push("scroll", lib.screen, obj.posX, obj.posY, 1, nikname)
                        elseif code == keyboard.keys.down then
                            event.push("scroll", lib.screen, obj.posX, obj.posY, -1, nikname)
                        end
                    end
                else
                    if eventName == "key_down" then
                        local posChange = false
                        if code == keyboard.keys.up then
                            obj.setBlick(false)
                            obj.posY = obj.posY - 1
                            posChange = true
                        elseif code == keyboard.keys.down then
                            obj.setBlick(false)
                            obj.posY = obj.posY + 1
                            posChange = true
                        elseif code == keyboard.keys.left then
                            obj.setBlick(false)
                            obj.posX = obj.posX - 1
                            posChange = true
                        elseif code == keyboard.keys.right then
                            obj.setBlick(false)
                            obj.posX = obj.posX + 1
                            posChange = true
                        end
                        if obj.posCheck() then
                            posChange = false
                        end

                        if posChange and obj.pressedButton then
                            event.push("drag", lib.screen, obj.posX, obj.posY, obj.pressedButton, nikname)
                        end
                    end
                    if eventName == "key_down" or eventName == "key_up" then
                        if not obj.pressedButton or eventName ~= "key_down" then
                            if code == keyboard.keys.enter then
                                if eventName == "key_down" then
                                    event.push("touch", lib.screen, obj.posX, obj.posY, 0, nikname)
                                    obj.pressedButton = 0
                                elseif eventName == "key_up" then
                                    event.push("drop", lib.screen, obj.posX, obj.posY, 0, nikname)
                                    obj.pressedButton = false
                                end
                            elseif code == keyboard.keys.tab then
                                if eventName == "key_down" then
                                    event.push("touch", lib.screen, obj.posX, obj.posY, 1, nikname)
                                    obj.pressedButton = 1
                                elseif eventName == "key_up" then
                                    event.push("drop", lib.screen, obj.posX, obj.posY, 1, nikname)
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

    --scene menager
    lib.scene = nil
    lib.scenes = {}

    function lib.createScene(sceneColor, sizeX, sizeY)
        local scene = {}
        scene.sceneColor = sceneColor or colors.black
        scene.sizeX = sizeX or lib.maxX
        scene.sizeY = sizeY or lib.maxY
        scene.redrawAll = false

        function scene.getResolution()
            return scene.sizeX, scene.sizeY
        end

        function scene.getCenter(posX, posY, sizeX, sizeY)
            if not posX then posX = 1 end
            if not posY then posY = 1 end
            if not sizeX then sizeX = scene.sizeX end
            if not sizeY then sizeY = scene.sizeY end
            local x = math.floor(posX + (sizeX // 2))
            local y = math.floor(posY + (sizeY // 2))
            return x, y
        end

        scene.threadsMenager = createThreadsMenager(scene)
        scene.timers = scene.threadsMenager.timers
        scene.listens = scene.threadsMenager.listens
        scene.threads = scene.threadsMenager.threads
        scene.createTimer = scene.threadsMenager.createTimer
        scene.createListen = scene.threadsMenager.createListen
        scene.createThread = scene.threadsMenager.createThread

        scene.objects = {}

        scene.openCallbacks = {}

        function scene.attachOpenCallback(func)
            table.insert(scene.openCallbacks, func)
        end
        function scene.dettachOpenCallback(func)
            table_remove(scene.openCallbacks, func)
        end

        -------------------------------------

        function scene.createButton(posX, posY, sizeX, sizeY, text, callback, mode, state)
            local obj = {}
            obj.subobjects = {}

            obj.posX = posX
            obj.posY = posY
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.text = text or ""
            obj.callback = callback
            obj.mode = mode or 0
            obj.state = orValue(state, false)

            obj.backColor = colors.white
            obj.foreColor = colors.black
            obj.invertBackColor = lib.selectColor(colors.gray, nil, false)
            obj.invertForeColor = colors.white

            obj.drawer = false

            obj.killed = false
            obj.active = true

            obj.invisible = false
            obj.vertText = false

            function obj.attachDrawer(drawer)
                obj.drawer = drawer
                obj.drawer.move(obj.posX, obj.posY)
                table_remove(scene.objects, drawer)
                table.insert(obj.subobjects, drawer)
            end

            function obj.draw(forceDraw, forceDraw2)
                if (not obj.active and not forceDraw) or obj.killed or obj.invisible then return end
                if lib.scene ~= scene or lib.block then return end --для корректной ручьной перерисовки
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end

                if obj.drawer then
                    obj.drawer.draw(forceDraw, forceDraw2)
                    return
                end

                local back, fore = obj.backColor, obj.foreColor
                if su.xor(not obj.state, obj.mode == 0 or obj.mode == 2) then back, fore = obj.invertBackColor, obj.invertForeColor end
                lib.gpu.setBackground(back)
                lib.gpu.setForeground(fore)
                lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")

                local posX, posY = scene.getCenter(obj.posX, obj.posY, obj.sizeX, obj.sizeY)
                if not obj.vertText then
                    posX = posX - unicode.len(obj.text) // 2
                else
                    posY = posY - unicode.len(obj.text) // 2
                end
                lib.gpu.set(posX, posY, obj.text, obj.vertText)
            end

            obj.listens = {}

            --не table.insert чтоб не добовлять много скобок
            obj.listens[#obj.listens + 1] = scene.createListen("touch", function(_, uuid, posX, posY, button, nikname)
                if not obj.active or obj.killed then return end
                if lib.block then return end --для того чтобы временно преостановить обработку, наример для контекстного меню
                if uuid == lib.screen and isZone(obj, posX, posY) then
                    if obj.mode == 0 then
                        obj.state = true
                        obj.draw()
                        if obj.soundOn then soundNum(0) end
                        computer.delay(0.1) --это не прирывания, а delay сохраняюший эвенты, добовляеться этот метод моим модом для openOS

                        obj.state = false
                        obj.draw()
                        if obj.soundOn then soundNum(1) end
                        computer.delay(0.1)

                        runCallback(obj.callback, true, false, button, nikname)
                    elseif obj.mode == 1 then
                        obj.state = not obj.state
                        obj.draw()
                        if obj.soundOn then soundNum(obj.state and 0 or 1) end
                        runCallback(obj.callback, obj.state, not obj.state, button, nikname)
                    elseif obj.mode == 2 then
                        obj.state = not obj.state
                        obj.draw()
                        if obj.soundOn then soundNum(obj.state and 0 or 1) end
                        runCallback(obj.callback, obj.state, not obj.state, button, nikname)
                    end
                    return
                end
                if uuid == lib.screen then
                    if obj.mode == 2 then
                        if obj.state then
                            obj.state = false
                            obj.draw()
                            if obj.soundOn then soundNum(1) end
                            runCallback(obj.callback, obj.state, not obj.state, button, nikname)
                        end
                    end
                end
            end)

            obj.listens[#obj.listens + 1] = scene.createListen("drop", function(_, uuid, posX, posY, button, nikname)
                if not obj.active or obj.killed then return end
                if lib.block then return end
                if uuid == lib.screen then
                    if obj.mode == 2 then
                        if obj.state then
                            obj.state = false
                            obj.draw()
                            if obj.soundOn then soundNum(1) end
                            runCallback(obj.callback, obj.state, not obj.state, button, nikname)
                        end
                    end
                end
            end)

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
                if obj.drawer then
                    obj.drawer.move(posX, posY)
                end
            end

            function obj.setActive(state)
                obj.active = state
                if obj.drawer then
                    obj.drawer.setActive(state)
                end
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            function obj.setVertText(state)
                obj.vertText = state
            end

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            function obj.remove()
                obj.killed = true
                for i = 1, #obj.listens do
                    obj.listens[i].kill()
                end
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createLabel(posX, posY, sizeX, sizeY, text)
            local obj = {}
            obj.subobjects = {}

            obj.posX = posX
            obj.posY = posY
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.text = text or ""
            obj.state = true

            obj.backColor = colors.white
            obj.foreColor = colors.black
            obj.invertBackColor = lib.selectColor(colors.gray, nil, false)
            obj.invertForeColor = colors.white

            obj.active = true
            obj.killed = false

            obj.invisible = false
            obj.vertText = false

            function obj.draw(forceDraw, forceDraw2)
                if (not obj.active and not forceDraw) or obj.killed or obj.invisible then return end
                if lib.scene ~= scene or lib.block then return end --для корректной ручьной перерисовки
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end

                local back, fore = obj.backColor, obj.foreColor
                if not obj.state then back, fore = obj.invertBackColor, obj.invertForeColor end
                lib.gpu.setBackground(back)
                lib.gpu.setForeground(fore)
                lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                
                local posX, posY = scene.getCenter(obj.posX, obj.posY, obj.sizeX, obj.sizeY)
                if not obj.vertText then
                    posX = posX - unicode.len(obj.text) // 2
                else
                    posY = posY - unicode.len(obj.text) // 2
                end
                lib.gpu.set(posX, posY, obj.text, obj.vertText)
            end

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
            end

            function obj.setVertText(state)
                obj.vertText = state
            end

            function obj.setActive(state)
                obj.active = state
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            function obj.remove()
                obj.killed = true
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createSeekbar(posX, posY, size, text, callback, mode, min, max, value, touch, onlyIntegers)
            local obj = {}
            obj.subobjects = {}

            obj.posX = posX
            obj.posY = posY
            obj.size = size
            obj.text = text and (text .. ":")
            obj.callback = callback

            obj.mode = mode or 0
            obj.touch = orValue(touch, true)
            obj.onlyIntegers = onlyIntegers
            obj.scrollCount = 1
            obj.scrollWheel = true
            obj.min = min or 0
            obj.max = max or 1
            obj.value = value or obj.min
            obj.floorAt = 0.1
            function obj.floor()
                if obj.onlyIntegers then
                    obj.value = math.floor(obj.value)
                else
                    obj.value = obj.value + 0.0
                end
            end
            obj.floor()
            
            if obj.text then
                obj.labelSize = unicode.len(obj.text) + unicode.len(tostring(obj.max)) + 3
                if obj.onlyIntegers then
                    obj.labelSize = obj.labelSize - 2
                end
            else
                obj.labelSize = 0
            end
            obj.realSize = obj.size - obj.labelSize

            obj.backColor = colors.white
            obj.foreColor = colors.black

            obj.killed = false
            obj.active = true

            obj.invisible = false

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            if obj.mode == 0 then
                obj.sizeX = obj.size --для обработки косаний
                obj.sizeY = 1
            else
                obj.sizeX = 1
                obj.sizeY = obj.size
            end

            local function getPointerPos()
                return math.ceil(su.mapClip(obj.value, obj.min, obj.max, 1, obj.realSize))
            end

            local function convertPointerPos(value)
                if obj.onlyIntegers then
                    return math.floor(su.mapClip(value, 1, obj.realSize, obj.min, obj.max))
                else
                    return su.floorAt(su.mapClip(value, 1, obj.realSize, obj.min, obj.max), obj.floorAt) + 0.0
                end
            end

            local function setPointerPos(value)
                obj.value = convertPointerPos(value)
            end

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
            end

            function obj.draw(forceDraw, forceDraw2)
                if (not obj.active and not forceDraw) or obj.killed or obj.invisible then return end
                if lib.scene ~= scene or lib.block then return end --для корректной ручьной перерисовки
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end

                lib.gpu.setBackground(obj.backColor)
                lib.gpu.setForeground(obj.foreColor)

                local str = obj.text and (obj.text .. tostring(obj.value))
                if obj.mode == 0 then
                    lib.gpu.fill(obj.posX, obj.posY, obj.size, 1, " ")
                    lib.gpu.fill(obj.posX + obj.labelSize, obj.posY, obj.size - obj.labelSize, 1, "—")
                    if str then
                        lib.gpu.set((obj.posX + obj.labelSize) - 1, obj.posY, ":")
                        lib.gpu.set(obj.posX, obj.posY, str)
                    end

                    lib.gpu.set(obj.posX + obj.labelSize + (getPointerPos() - 1), obj.posY, "⬤")
                else
                    lib.gpu.fill(obj.posX, obj.posY, 1, obj.size, " ")
                    lib.gpu.fill(obj.posX, obj.posY + obj.labelSize, 1, obj.size - obj.labelSize, "│")
                    if str then
                        lib.gpu.set(obj.posX, (obj.posY + obj.labelSize) - 1, ":")
                        lib.gpu.set(obj.posX, obj.posY, str, true)
                    end

                    if obj.mode == 1 then
                        lib.gpu.set(obj.posX, obj.posY + obj.labelSize + (getPointerPos() - 1), "⬤")
                    else
                        lib.gpu.set(obj.posX, obj.posY + obj.labelSize + (obj.realSize - getPointerPos()), "⬤")
                    end
                end
            end

            obj.isPress = false

            obj.listens = {}

            obj.listens[#obj.listens + 1] = scene.createListen(nil, function(eventName, uuid, posX, posY, button)
                if not obj.active or obj.killed then return end
                if lib.block then return end
                if uuid ~= lib.screen or (button ~= 0 and eventName ~= "scroll") or not obj.touch then return end
                if eventName == "scroll" and obj.scrollWheel then
                    if isZone(obj, posX, posY) then
                        local oldValue = obj.value

                        obj.value = obj.value + (button * obj.scrollCount)
                        if obj.value > obj.max then obj.value = obj.max end
                        if obj.value < obj.min then obj.value = obj.min end
                        obj.floor()

                        if obj.value == oldValue then return end
                        obj.draw()
                        if obj.soundOn then soundNum(2) end
                        runCallback(obj.callback, obj.value, oldValue)
                    end
                end
                if eventName == "drop" then
                    obj.isPress = false
                    return
                end
                if eventName == "touch" then
                    if isZone(obj, posX, posY) then
                        obj.isPress = true
                    end
                    if not obj.isPress then return end
                end
                
                if eventName == "touch" or eventName == "drag" then
                    if obj.isPress then
                        local pos
                        if obj.mode == 0 then
                            pos = (posX + 1) - (obj.posX + obj.labelSize)
                        elseif obj.mode == 1 then
                            pos = (posY + 1) - (obj.posY + obj.labelSize)
                        elseif obj.mode == 2 then
                            pos = obj.realSize - ((posY + 0) - (obj.posY + obj.labelSize))
                        end
                        local value = convertPointerPos(pos)

                        if value > obj.max then value = obj.max end
                        if value < obj.min then value = obj.min end
                        obj.floor()

                        local oldValue = obj.value
                        obj.value = value
                        if value == oldValue then return end
                        obj.draw()
                        if obj.soundOn then soundNum(2) end
                        runCallback(obj.callback, obj.value, oldValue)
                    end
                end
            end)

            function obj.setActive(state)
                obj.active = state
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            function obj.remove()
                obj.killed = true
                for i = 1, #obj.listens do
                    obj.listens[i].kill()
                end
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createList(posX, posY, sizeX, sizeY, callback)
            local obj = {}
            obj.subobjects = {}

            obj.posX = posX
            obj.posY = posY
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.callback = callback

            obj.backColor = colors.white
            obj.foreColor = colors.black

            obj.autoScroll = true
            obj.autoRedraw = true
            obj.autoRemove = true
            obj.maxStrsCount = (obj.sizeY ^ 2) // 2

            obj.seekBar = scene.createSeekbar((obj.posX + obj.sizeX - 1), obj.posY, obj.sizeY, nil, function()
                obj.draw()
            end, 2, 0, obj.sizeY - 1, 0, true, true)
            table_remove(scene.objects, obj.seekBar)
            table.insert(obj.subobjects, obj.seekBar)
            obj.seekBar.scrollWheel = false
            if obj.sizeY >= 4 then
                obj.seekBar.scrollCount = 2
            elseif obj.sizeY >= 8 then
                obj.seekBar.scrollCount = 3
            elseif obj.sizeY >= 10 then
                obj.seekBar.scrollCount = 4
            end

            obj.strs = {}
            obj.screenStrs = {}
            obj.screenStrsNums = {}

            obj.killed = false
            obj.active = true

            obj.invisible = false

            function obj.draw(forceDraw, forceDraw2)
                if (not obj.active and not forceDraw) or obj.killed then return end
                if lib.scene ~= scene or lib.block then return end --для корректной ручьной перерисовки
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 and not obj.invisible then lib.redraw() return end

                if not obj.invisible then
                    lib.gpu.setBackground(obj.backColor)
                    lib.gpu.setForeground(obj.foreColor)
                    lib.gpu.fill(obj.posX, obj.posY, obj.sizeX - 1, obj.sizeY, " ")
                end

                obj.screenStrs = {}
                obj.screenStrsNums = {}
                local mainStr = (obj.posY + obj.sizeY) - 1
                local scroll = obj.seekBar.value
                for i = 1, #obj.strs do
                    local posY = (mainStr - (i - 1)) + su.map(scroll, obj.seekBar.min, obj.seekBar.max, 0, #obj.strs)
                    posY = math.floor(posY)
                    if posY >= obj.posY and posY <= mainStr then
                        local str = obj.strs[(#obj.strs - i) + 1]
                        str = unicode.sub(str, 1, obj.sizeX - 1)
                        obj.screenStrs[posY - obj.posY] = str
                        obj.screenStrsNums[posY - obj.posY] = (#obj.strs - i) + 1
                        if not obj.invisible then lib.gpu.set(obj.posX, posY, str) end
                    end
                end

                if not obj.invisible then obj.seekBar.draw(forceDraw, forceDraw2) end
            end

            function obj.reMatch()
                if obj.autoRemove and #obj.strs > obj.maxStrsCount then
                    table.remove(obj.strs, 1)
                end
                local oldMax = obj.seekBar.max
                obj.seekBar.max = #obj.strs
                if obj.autoScroll then
                    obj.seekBar.value = 0
                    obj.seekBar.floor()
                else
                    obj.seekBar.value = math.ceil(su.mapClip(obj.seekBar.value, obj.seekBar.min, oldMax, obj.seekBar.min, obj.seekBar.max))
                    obj.seekBar.floor()
                end
                if obj.autoRedraw then
                    obj.draw()
                    obj.seekBar.draw()
                end
            end

            function obj.addStr(str)
                table.insert(obj.strs, str)
                obj.reMatch()
            end

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            function obj.clear()
                for i = 1, #obj.strs do
                    obj.strs[i] = nil
                end
                obj.seekBar.value = 0
                obj.seekBar.floor()
                if obj.autoRedraw then
                    obj.draw()
                    obj.seekBar.draw()
                end
            end

            function obj.removeAtStr(str)
                table_remove(obj.strs, str)
                obj.reMatch()
            end

            function obj.removeAtNum(num)
                table.remove(obj.strs, num)
                obj.reMatch()
            end

            function obj.exists(name)
                for i = 1, #obj.strs do
                    if obj.strs[i] == name then
                        return true
                    end
                end
                return false
            end

            obj.listens = {}

            obj.listens[#obj.listens + 1] = scene.createListen(nil, function(eventName, uuid, posX, posY, button)
                if lib.block then return end
                if not obj.active or obj.killed then return end
                if uuid ~= lib.screen or not obj.seekBar.touch then return end
                if eventName == "scroll" then
                    if isZone(obj, posX, posY) then
                        local oldValue = obj.seekBar.value

                        obj.seekBar.value = obj.seekBar.value + (button * obj.seekBar.scrollCount)
                        if obj.seekBar.value > obj.seekBar.max then obj.seekBar.value = obj.seekBar.max end
                        if obj.seekBar.value < obj.seekBar.min then obj.seekBar.value = obj.seekBar.min end

                        if obj.seekBar.value == oldValue then return end
                        obj.draw()
                        if obj.soundOn then soundNum(2) end
                    end
                end
                if eventName == "touch" then
                    if isZone({posX = obj.posX, posY = obj.posY, sizeX = obj.sizeX - 1, sizeY = obj.sizeY}, posX, posY) then
                        posY = math.floor(posY)
                        local num = posY - obj.posY
                        if preciseState then num = num + 1 end
                        local str = obj.screenStrs[num]
                        local strnum = obj.screenStrsNums[num]
                        if str then
                            runCallback(obj.callback, str, math.floor(button), strnum)
                        end
                    end
                end
            end)

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
                obj.seekBar.posX = (obj.posX + obj.sizeX - 1)
                obj.seekBar.posY = obj.posY
            end

            function obj.setActive(state)
                obj.active = state
                obj.seekBar.active = state
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            function obj.remove()
                obj.killed = true
                for i = 1, #obj.listens do
                    obj.listens[i].kill()
                end
                obj.seekBar.remove()
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createInputbox(posX, posY, sizeX, sizeY, text, callback)
            local obj = {}
            obj.subobjects = {}

            obj.userInput = nil
            obj.text = text
            obj.viewData = true
            obj.callback = callback
            obj.used = false

            obj.active = true
            obj.killed = false

            obj.invisible = false

            function obj.input(nikname)
                if lib.scene ~= scene or lib.block then return end
                if not obj.active or obj.killed then return end
                if obj.used then return end

                scene.createThread(function()
                    obj.used = true
                    obj.button.text = ""
                    obj.button.draw()
                    term.setCursor(obj.button.posX, obj.button.posY)
                    local read = term.read() --не io.read потому что он криво работает с патоками

                    local function setText()
                        if obj.userInput and obj.viewData then
                            obj.button.text = obj.text .. ":" .. obj.userInput
                        else
                            obj.button.text = obj.text
                        end
                    end

                    if not read then
                        setText()
                        lib.redraw()
                        obj.ending = true
                        return
                    end
                    if unicode.sub(read, unicode.len(read), unicode.len(read)) == "\n" then --вдруг поведения билиотеки измениться
                        read = unicode.sub(read, 1, unicode.len(read) - 1)
                    end
                    obj.userInput = read
                    setText()
                    lib.redraw()
                    obj.ending = true
                    obj.ok = true
                end)
                scene.createTimer(0.2, function()
                    if obj.ending then
                        obj.used = false
                        obj.ending = nil
                        if obj.ok then
                            runCallback(obj.callback, obj.userInput, nikname)
                        else
                            runCallback(obj.cancel, nikname)
                        end
                        obj.ok = nil
                        return false
                    end
                end)
            end

            obj.button = scene.createButton(posX, posY, sizeX, sizeY, text, function(_, _, _, nikname)
                obj.input(nikname)
            end)
            table_remove(scene.objects, obj.button)
            table.insert(obj.subobjects, obj.button)

            function obj.move(posX, posY)
                obj.button.posX = posX
                obj.button.posY = posY
            end

            function obj.draw(forceDraw, forceDraw2)
                if lib.scene ~= scene or lib.block or obj.invisible then return end --для корректной ручьной перерисовки
                if (not obj.active and not forceDraw) or obj.killed then return end
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end
                obj.button.draw(forceDraw, forceDraw2)
            end

            function obj.setActive(state)
                obj.active = state
                obj.button.active = state
            end

            function obj.setVertText(state)
                obj.button.vertText = state
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            function obj.remove()
                obj.killed = true
                obj.button.remove()
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createDrawer(posX, posY, drawer)
            local obj = {}
            obj.subobjects = {}

            obj.posX = posX
            obj.posY = posY
            obj.drawer = drawer

            obj.active = true
            obj.killed = false

            obj.invisible = false

            function obj.draw(forceDraw, forceDraw2)
                if lib.scene ~= scene or lib.block or obj.invisible then return end --для корректной ручьной перерисовки
                if (not obj.active and not forceDraw) or obj.killed then return end
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end

                runCallback(obj.drawer, lib.gpu, obj.posX, obj.posY)
            end

            function obj.setActive(state)
                obj.active = state
            end

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            function obj.remove()
                obj.killed = true
                table_remove(scene.objects, obj)
            end

            table.insert(scene.objects, obj)
            return obj
        end

        function scene.createWindow(posX, posY, sizeX, sizeY)
            local obj = {}
            obj.subobjects = {}

            obj.userMove = false
            
            obj.posX = posX
            obj.posY = posY
            obj.sizeX = sizeX
            obj.sizeY = sizeY
            obj.color = nil

            obj.active = true
            obj.killed = false

            obj.windowSelected = false

            obj.invisible = false

            obj.objects = {}

            local function windowMenager(state)
                if state == obj.windowSelected then return end
                obj.windowSelected = state

                for i, object in ipairs(scene.objects) do
                    if not object.backToClassic or object == obj then goto continue end
                    object.backToClassic()
                    ::continue::
                end
                if state then
                    --for i, object in ipairs(scene.objects) do
                    --    if object ~= obj and (object.backToClassic or object.windowSelected) then return end
                    --end
                    for i, object in ipairs(scene.objects) do
                        if object.backToClassic or object == obj then goto continue end
                        object.oldState = object.active
                        object.setActive(false)
                        object.oldSetActive = object.setActive
                        object.setActive = function(state)
                            object.oldState = state
                        end
                        object.backToClassic = function()
                            object.setActive = object.oldSetActive
                            object.oldSetActive = nil
                            object.setActive(object.oldState)
                            object.backToClassic = nil
                        end
                        ::continue::
                    end
                end
            end
            obj.windowMenager = windowMenager

            function obj.attachObj(posX, posY, tbl)
                table.insert(obj.objects, {posX, posY, tbl})
                table_remove(scene.objects, tbl)
                table.insert(obj.subobjects, tbl)
            end

            function obj.draw(forceDraw, forceDraw2)
                if lib.scene ~= scene or lib.block or obj.invisible then return end --для корректной ручьной перерисовки
                if (not obj.active and not forceDraw) or obj.killed then return end
                if (lib.redrawAll or scene.redrawAll) and not forceDraw2 then lib.redraw() return end

                if obj.color then
                    lib.gpu.setBackground(obj.color)
                    lib.gpu.fill(obj.posX, obj.posY, obj.sizeX, obj.sizeY, " ")
                end

                for i = 1, #obj.objects do
                    local nobj = obj.objects[i]
                    local cobj = nobj[3]
                    cobj.move(obj.posX + (nobj[1] - 1), obj.posY + (nobj[2] - 1))
                    cobj.draw(forceDraw, forceDraw2) --для того чтобы после отрисовки окна все его элементы перересовывались
                end
            end

            function obj.setActive(state)
                obj.active = state
                for i = 1, #obj.objects do
                    obj.objects[i][3].setActive(state)
                end
            end

            obj.soundOn = true
            function obj.setSoundOn(state)
                obj.soundOn = state
                for i, object in ipairs(obj.subobjects) do
                    object.soundOn = state
                end
            end

            function obj.move(posX, posY)
                obj.posX = posX
                obj.posY = posY
            end

            function obj.setInvisible(state)
                obj.invisible = state
                for i = 1, #obj.subobjects do
                    obj.subobjects[i].setInvisible(state)
                end
            end

            obj.listens = {}

            obj.isPress = false
            obj.pressX = -1
            obj.pressY = -1

            obj.listens[#obj.listens + 1] = scene.createListen(nil, function(eventName, uuid, touchX, touchY, button, nik)
                if lib.block then return end
                if not obj.active or obj.killed then return end
                if uuid ~= lib.screen then return end

                if obj.userMove then
                    if eventName == "touch" or eventName == "drag" or eventName == "scroll" then
                        if isZone(obj, touchX, touchY) then
                            windowMenager(true)
                        else
                            if obj.windowSelected then
                                windowMenager(false)
                                event.push(eventName, uuid, touchX, touchY, button, nik)
                            end
                        end
                    end
                    if button ~= 0 then return end
                    if eventName == "drop" then
                        obj.isPress = false
                        return
                    elseif eventName == "touch" then
                        if isZone(obj, touchX, touchY) then
                            obj.isPress = true
                            obj.pressX = touchX
                            obj.pressY = touchY
                        end
                    elseif eventName == "drag" then
                        if obj.isPress then
                            local dx = touchX - obj.pressX
                            local dy = touchY - obj.pressY
                            obj.pressX = touchX
                            obj.pressY = touchY

                            obj.move(obj.posX + dx, obj.posY + dy)
                            lib.redraw()
                        end
                    end
                end
            end)

            function obj.remove()
                if obj.windowSelected then
                    windowMenager(false)
                end

                obj.killed = true
                for i = 1, #obj.objects do
                    obj.objects[i][3].remove()
                end
                for i = 1, #obj.listens do
                    obj.listens[i].kill()
                end
                table_remove(scene.objects, obj)
            end

            --for i, v in ipairs(scene.objects) do
            --    if v.windowSelected then
            --        v.windowMenager(false)
            --    end
            --end
            --windowMenager(true)
            
            table.insert(scene.objects, obj)
            return obj
        end

        -------------------------------------

        function scene.draw()
            if lib.block or lib.scene ~= scene then return end --обработка заблокированного состояния gui

            lib.gpu.setResolution(scene.sizeX, scene.sizeY)
            lib.gpu.setBackground(scene.sceneColor)
            term.clear()

            for i, data in ipairs(scene.objects) do
                if data.windowSelected == nil then --нет not не подайдет, так как тогда false тоже вернет true
                    data.draw(not not data.backToClassic, (lib.redrawAll or scene.redrawAll)) --not not это чтоб boolean были а не таблицы
                end
            end

            for i, data in ipairs(scene.objects) do
                if data.windowSelected ~= nil then
                    data.draw(not not data.backToClassic, (lib.redrawAll or scene.redrawAll)) --not not это чтоб boolean были а не таблицы
                end
            end
        end

        function scene.remove()
            lib.removeScene(scene)
        end

        table.insert(lib.scenes, scene)
        return scene
    end

    function lib.removeScene(sceneOrNumber)
        if type(sceneOrNumber) == "table" then
            for i = 1, #lib.scenes do
                if lib.scenes[i] == sceneOrNumber then
                    sceneOrNumber = i
                    break
                end
            end
        end
        local scene = lib.scenes[sceneOrNumber]
        scene.threadsMenager.killAll()
        while true do
            if not scene.objects[1] then break end
            scene.objects[1].remove()
        end
        table.remove(lib.scenes, sceneOrNumber)
    end

    function lib.select(sceneOrNumber)
        if not sceneOrNumber then
            if lib.scene then lib.scene.threadsMenager.stopAll() end
            lib.scene = nil
            return
        end
        if type(sceneOrNumber) == "number" then
            sceneOrNumber = lib.scenes[sceneOrNumber]
        end

        if sceneOrNumber ~= lib.scene then
            if lib.scene then lib.scene.threadsMenager.stopAll() end
            lib.scene = sceneOrNumber
            if lib.scene then lib.scene.threadsMenager.startAll() end
        end
        for i, data in ipairs(lib.scene.openCallbacks) do runCallback(data) end
        lib.redraw()
        lib.startTime = computer.uptime() + 0.2 --фикс паразитного нажатия кнопок при переключении сцен
    end

    --callback object
    lib.threadsMenager = createThreadsMenager()
    lib.timers = lib.threadsMenager.timers
    lib.listens = lib.threadsMenager.listens
    lib.threads = lib.threadsMenager.threads
    lib.createTimer = lib.threadsMenager.createTimer
    lib.createListen = lib.threadsMenager.createListen
    lib.createThread = lib.threadsMenager.createThread

    lib.lastTouch = {}
    lib.lastDrop = {}
    lib.lastDrag = {}
    lib.lastScroll = {}
    lib.lastAny = {}

    lib.createListen(nil, function(eventName, uuid, ...)
        if uuid == lib.screen then
            if eventName == "touch" then
                lib.lastTouch = {eventName, uuid, ...}
                lib.lastAny = {eventName, uuid, ...}
            elseif eventName == "drop" then
                lib.lastDrop = {eventName, uuid, ...}
                lib.lastAny = {eventName, uuid, ...}
            elseif eventName == "drag" then
                lib.lastDrag = {eventName, uuid, ...}
                lib.lastAny = {eventName, uuid, ...}
            elseif eventName == "scroll" then
                lib.lastScroll = {eventName, uuid, ...}
                lib.lastAny = {eventName, uuid, ...}
            end
        end
    end)

    --control
    function lib.redraw()
        if lib.block then return end --обработка заблокированного состояния gui
        if lib.scene then lib.scene.draw() end
    end

    function lib.start()
        lib.threadsMenager.startAll()
        if lib.scene then lib.scene.threadsMenager.startAll() end
    end

    function lib.stop()
        lib.threadsMenager.stopAll()
        if lib.scene then lib.scene.threadsMenager.stopAll() end
    end

    function lib.run()
        lib.start()
        while lib.active do
            os.sleep(1)
        end
    end

    --exit code
    lib.exitCallbacks = {}

    function lib.attachExitCallback(func)
        table.insert(lib.exitCallbacks, func)
    end
    function lib.dettachExitCallback(func)
        table_remove(lib.exitCallbacks, func)
    end
    
    local oldHook
    function lib.off()
        if not lib.active then return end
        pcall(component.invoke, lib.screen, "setPrecise", oldPreciseState)
        lib.active = false

        lib.threadsMenager.killAll()
        while true do
            if not lib.scenes[1] then break end
            lib.scenes[1].remove()
        end

        local oldHookState = event.superHook
        event.superHook = false
        for i = 1, 2 do os.sleep(0.1) end
        event.superHook = oldHookState

        process.info().data.signal = oldHook
        
        for i, data in ipairs(lib.exitCallbacks) do runCallback(data) end

        lib.reset_gpu()
        lib.gpu.setBackground(colors.black)
        lib.gpu.setForeground(colors.white)
        
        term.clear()
    end
    function lib.exit()
        lib.off()
        os.exit()
    end
    oldHook = process.info().data.signal
    process.info().data.signal = lib.exit

    --bloked functions
    function lib.context(posX, posY, inputData)
        lib.block = true

        local function getStr(num)
            local dat = inputData[num]
            if not dat then return nil end
            local str
            if type(dat) == "table" then
                str = dat[1]
            else
                dat = tostring(dat)
                str = dat
            end
            return str
        end

        local menuData = {strs = {}, on = {}}
        local sizeX, sizeY = 0
        for i = 1, #inputData do
            local dat = inputData[i]
            local str
            if type(dat) == "table" then
                str = dat[1]
                table.insert(menuData.strs, str)
                table.insert(menuData.on, dat[2])
            else
                dat = tostring(dat)
                str = dat
                table.insert(menuData.strs, dat)
                table.insert(menuData.on, true)
            end
            if unicode.len(str) > sizeX then sizeX = unicode.len(str) end
        end
        for i = 1, #menuData.strs do
            local str = menuData.strs[i]
            if unicode.len(str) < sizeX then
                menuData.strs[i] = menuData.strs[i] .. string.rep(" ", sizeX - unicode.len(str))
            end
        end
        sizeY = #menuData.strs

        local rx, ry = lib.gpu.getResolution()
        if posX + (sizeX - 1) > rx then posX = posX - (sizeX - 1) end
        if posY + (sizeY - 1) > ry then posY = posY - (sizeY - 1) end

        lib.gpu.setBackground(lib.selectColor(colors.gray, nil, false))
        lib.gpu.setForeground(lib.selectColor(colors.black, nil, true))
        local char = " "
        if lib.depth == 1 then
            char = "#"
        end
        lib.gpu.fill(posX + 1, posY + 1, sizeX, sizeY, char)

        for i = 1, sizeY do
            local pos = posY + (i - 1)
            if lib.depth == 1 then
                lib.gpu.setBackground(menuData.on[i] and colors.white or colors.black)
                lib.gpu.setForeground(menuData.on[i] and colors.black or colors.white)
            else
                lib.gpu.setBackground(colors.white)
                if menuData.on[i] then
                    lib.gpu.setForeground(colors.black)
                else
                    lib.gpu.setForeground(lib.selectColor(colors.lightGray, nil, false))
                end
            end
            lib.gpu.set(posX, pos, menuData.strs[i])
        end

        local out
        while true do
            local eventName, uuid, touchX, touchY, button, nikname = event.pull()
            if uuid == lib.screen and (eventName == "touch" or eventName == "scroll") then
                if eventName == "touch" and button == 0 and isZone({posX = posX, posY = posY, sizeX = sizeX, sizeY = sizeY}, touchX, touchY) then
                    local num = (touchY - posY) + 1
                    if menuData.on[num] then
                        out = num
                        break
                    end
                else
                    event.push(eventName, lib.screen, touchX, touchY, button, nikname)
                    break
                end
            end
        end

        lib.block = false
        lib.redraw()

        return getStr(out), out
    end

    --utiles
    function lib.pushScene(scene, time)
        local oldScene = lib.scene
        lib.select(scene)
        os.sleep(time or 2)
        if oldScene then lib.select(oldScene) end
    end

    function lib.splash(text, color, time, sx, sy)
        if not sx then sx = lib.maxX end
        if not sy then sy = lib.maxY end
        if not time then time = 2 end
        if not color then color = colors.white end

        local scene = lib.createScene(color, sx, sy)
        if text then
            local x, y = scene.getCenter()
            scene.createLabel(1, y, sx, 1, text)
        end
        lib.pushScene(scene, time)
        scene.remove()
    end

    function lib.yesno(text, sx, sy)
        if not sx then sx = lib.maxX end
        if not sy then sy = lib.maxY end

        local scene = lib.createScene(colors.white, sx, sy)
        local cx, cy = scene.getCenter()

        if text then scene.createLabel(1, 1, sx, 1, text) end

        local state = nil

        local yes = scene.createButton(1, (cy - 1) - 2, sx, 3, "yes", function() state = true end)
        local no = scene.createButton(1, (cy - 1) + 2, sx, 3, "no", function() state = false end)
        yes.backColor = lib.selectColor(colors.green, nil, false)
        yes.foreColor = colors.white
        yes.invertBackColor = lib.selectColor(colors.blue, nil, true)
        yes.invertForeColor = 0

        no.backColor = lib.selectColor(colors.red, nil, false)
        no.foreColor = colors.white
        no.invertBackColor = lib.selectColor(colors.blue, nil, true)
        no.invertForeColor = 0

        local oldScene = lib.scene
        lib.select(scene)
        
        while state == nil do os.sleep(0.1) end

        if oldScene then lib.select(oldScene) end
        scene.remove()

        return state
    end

    function lib.status(text, sx, sy)
        lib.gpu.setResolution(sx or lib.userX, sy or lib.userY)
        lib.gpu.setBackground(lib.selectColor(colors.white, nil, true))
        lib.gpu.setForeground(lib.selectColor(colors.lightGray, nil, false))
        lib.gpu.fill(1, 1, sx or lib.userX, sy or lib.userY, " ")
        lib.gpu.set(math.ceil(((sx or lib.userX) / 2) - (unicode.len(text) / 2)), math.ceil((sy or lib.userY) / 2), text)
    end

    -------------------------------------twicks active

    if (_G.alwaysUseCourses or lib.noTouch) and not _G.rcCursor then
        createCursor()
    end

    return lib
end}