local su = require("superUtiles")
local component = require("component")
local computer = require("computer")
local term = require("term")
local serialization = require("serialization")
local fs = require("filesystem")
local event = require("event")
local sha256 = require("sha256lib").sha256
local shell = require("shell")

local args, options = shell.parse(...)

-------------------------------------------

if not fs.exists("/etc/lock.cfg") then
    su.saveFile("/etc/lock.cfg", serialization.serialize({
        lock = false,
        passwordSha256 = "9af15b336e6a9619928537df30b2e6a2376569fcf9d7e773eccede65606529a0", --0000
        users = {},
        mainuser = false,
        adminAllow = false
    }))
end
local lockCfg = serialization.unserialize(su.getFile("/etc/lock.cfg"))
local function saveCfg()
    su.saveFile("/etc/lock.cfg", serialization.serialize(lockCfg))
end

-------------------------------------------

local function unlockScreen(skipAllow, powerOff)
    if not lockCfg.lock and not skipAllow and options.c then
        return
    end
    if not term.isAvailable() then
        computer.shutdown()
        return
    end

    local state = false

    if --[[math.floor(computer.getDeviceInfo()[term.screen()].width) == 1 or]] options.s then
        if not term.keyboard() then
            print("не найдена клавиатура, необходимая для авторизации")
            computer.shutdown()
            return
        end

        while true do
            term.clear()
            print("выберете способ авторизации")
            print("1.пароль")
            print("2.ник")
            if powerOff then
                print("3.выключить устройста")
            end
            if skipAllow then
                print("4.выйти")
            end
            print("enter: ")

            local num, nikname
            repeat
                local _, _, char, _, nikname2 = term.pull("key_down", term.keyboard())
                nikname = nikname2
                num = tonumber(string.char(math.floor(char)))
            until num and (num == 1 or num == 2 or (num == 3 and powerOff) or (num == 4 and skipAllow))

            if num == 1 then
                print("введите пароль")
                while true do
                    local inputData = io.read()
                    if not inputData then
                        break
                    end
                    if sha256(inputData) == lockCfg.passwordSha256 then
                        su.logTo("/free/logs/lock.log",
                            "password entered" .. ((nikname and (", player: " .. nikname)) or ""))
                        term.clear()
                        return true
                    else
                        su.logTo("/free/logs/lock.log",
                            "uncoreect password: " .. inputData .. ((nikname and (", player: " .. nikname)) or ""))
                        print("неверный пароль")
                    end
                end
            elseif num == 2 then
                if su.inTable(lockCfg.users, nikname) then
                    su.logTo("/free/logs/lock.log", "auto login user: " .. nikname)
                    print("с возврашениям " .. nikname)
                    os.sleep(2)
                    return true
                elseif not su.inTable({computer.users()}, nikname) and #({computer.users()}) ~= 0 then
                    if lockCfg.adminAllow then
                        su.logTo("/free/logs/lock.log", "auto login admin: " .. nikname)
                        print("разблокировано по админ доступу")
                        os.sleep(2)
                        return true
                    else
                        su.logTo("/free/logs/lock.log", "err auto login admin: " .. nikname)
                        print("админ доступ выключен, обратитесь к владельцу пк")
                    end
                else
                    su.logTo("/free/logs/lock.log", "err auto login user: " .. nikname)
                    print("вас нету в таблице пользователей")
                end
            elseif num == 3 then
                if powerOff then
                    computer.shutdown()
                else
                    print("действия запрешено")
                end
            elseif num == 4 then
                return
            end
            print("press enter or wait 4 seconds to continue...")
            event.pull(4, "key_down", term.keyboard(), nil, 28)
        end
    else
        local gui = require("guix").create()

        local mx, my = 50, 16
        if component.isAvailable("tablet") then
            mx, my = gui.maxX, gui.maxY
        end

        local main = gui.createScene(gui.selectColor(0xFFFFFF, nil, false), mx, my)
        local cx, cy = main.getCenter()

        local i = main.createInputbox(cx - (16 // 2), cy - 1, 16, 1, "enter password", function(inputData, nikname)
            if sha256(inputData) == lockCfg.passwordSha256 then
                su.logTo("/free/logs/lock.log", "password entered" .. ((nikname and (", player: " .. nikname)) or ""))
                state = true
                gui.off()
            else
                su.logTo("/free/logs/lock.log",
                    "uncoreect password: " .. inputData .. ((nikname and (", player: " .. nikname)) or ""))

                local textScene = gui.createScene(gui.selectColor(0xFF0000, nil, false), mx, my)
                local cx, cy = textScene.getCenter()
                textScene.createLabel(1, cy, mx, 1, "неверный пароль")
                local oldScene = gui.scene
                gui.select(textScene)
                os.sleep(2)
                gui.select(oldScene)
                textScene.remove()
            end
        end)
        i.viewData = false
        i.button.backColor = gui.selectColor(0xAAAAAA, nil, true)

        local autobutton = main.createButton(cx - (16 // 2), cy + 1, 16, 1, "auto", function(_, _, _, nikname)
            if su.inTable(lockCfg.users, nikname) then
                su.logTo("/free/logs/lock.log", "auto login user: " .. nikname)

                local textScene = gui.createScene(gui.selectColor(0x00FF00, nil, false), mx, my)
                local cx, cy = textScene.getCenter()
                textScene.createLabel(1, cy, mx, 1, "с возврашениям " .. nikname)
                local oldScene = gui.scene
                gui.select(textScene)
                os.sleep(2)
                -- gui.select(oldScene)
                state = true
                gui.off()
            else
                if not su.inTable({computer.users()}, nikname) and #({computer.users()}) ~= 0 then
                    if lockCfg.adminAllow then
                        su.logTo("/free/logs/lock.log", "auto login admin: " .. nikname)

                        local textScene = gui.createScene(gui.selectColor(0x00FF00, nil, false), mx, my)
                        local cx, cy = textScene.getCenter()
                        textScene.createLabel(1, cy, mx, 1,
                            "разблокировано по админ доступу")
                        local oldScene = gui.scene
                        gui.select(textScene)
                        os.sleep(2)
                        -- gui.select(oldScene)
                        state = true
                        gui.off()
                    else
                        su.logTo("/free/logs/lock.log", "err auto login admin: " .. nikname)

                        local textScene = gui.createScene(gui.selectColor(0xFF0000, nil, false), mx, my)
                        local cx, cy = textScene.getCenter()
                        textScene.createLabel(1, cy, mx, 1,
                            "админ доступ выключен, обратитесь к владельцу пк")
                        if lockCfg.mainuser then
                            textScene.createLabel(1, cy + 2, mx, 1, "владелец (" .. lockCfg.mainuser .. ")")
                        end
                        local oldScene = gui.scene
                        gui.select(textScene)
                        os.sleep(2)
                        gui.select(oldScene)
                        textScene.remove()
                    end
                else
                    su.logTo("/free/logs/lock.log", "err auto login user: " .. nikname)

                    local textScene = gui.createScene(gui.selectColor(0xFF0000, nil, false), mx, my)
                    local cx, cy = textScene.getCenter()
                    textScene.createLabel(1, cy, mx, 1, "вас нету в таблице пользователей")
                    if lockCfg.mainuser then
                        textScene.createLabel(1, cy + 2, mx, 1, "владелец (" .. lockCfg.mainuser .. ")")
                    end
                    local oldScene = gui.scene
                    gui.select(textScene)
                    os.sleep(2)
                    gui.select(oldScene)
                    textScene.remove()
                end
            end
        end)
        autobutton.backColor = gui.selectColor(0xAAAAAA, nil, true)

        if powerOff then
            local offbutton = main.createButton(1, 1, 5, 1, "off", function(_, _, _, nikname)
                su.logTo("/free/logs/lock.log", "computer off: " .. nikname)
                computer.shutdown()
            end)
            offbutton.backColor = gui.selectColor(0xFF0000, nil, true)
        end

        if skipAllow then
            local exitbutton = main.createButton(1, 1, 5, 1, "exit", function(_, _, _, nikname)
                state = false
                gui.off()
            end)
            exitbutton.backColor = gui.selectColor(0x00FF00, nil, true)
        end

        main.createTimer(0.05, function()
            i.draw()
        end)

        gui.select(main)
        gui.run()
    end

    return state
end

local function usersMenager()
    if --[[math.floor(computer.getDeviceInfo()[term.screen()].width) == 1 or]] options.s then
        while true do
            term.clear()
            print("users")
            for i, data in ipairs(lockCfg.users) do
                print(tonumber(i) .. "." .. data)
            end
            print("menu")
            print("1.clear")
            print("2.add")
            print("3.remove")
            print("4.user add")
            print("5.save and exit")
            local num = io.read()
            if not num or num == 5 then
                break
            end
            num = tonumber(num)
            if num == 1 then
                for i = 1, #lockCfg.users do
                    lockCfg.users[i] = nil
                end
            elseif num == 2 then
                local newNik = io.read()
                if newNik then
                    if su.inTable(lockCfg.users, newNik) then
                        print("уже в таблице")
                    else
                        table.insert(lockCfg.users, newNik)
                    end
                end
            elseif num == 3 then
                local num = tonumber(io.read())
                if num then
                    table.remove(lockCfg.users, num)
                end
            elseif num == 4 then
                print("нажмите enter")
                local _, _, _, _, nik = event.pull("key_down", term.keyboard(), nil, 28)
                if su.inTable(lockCfg.users, nik) then
                    print("вы уже в таблице")
                else
                    print("добавить вас " .. nik .. "? [Y/n]")
                    local read = io.read()
                    if read and (read == "Y" or read == "y") then
                        table.insert(lockCfg.users, nik)
                    end
                end
            end
            print("press enter or wait 4 seconds to continue...")
            event.pull(4, "key_down", term.keyboard(), nil, 28)
        end
        saveCfg()
    else
        local gui = require("guix").create()
        gui.redrawAll = true

        local main = gui.createScene(0, gui.userX, gui.userY)

        local function createWindow(text)
            local window = main.createWindow(5, 5, 50, 10)
            window.color = 0xAAAAAA
            window.userMove = true

            local label = main.createLabel(1, 1, 50, 1, text)
            window.attachObj(1, 5, label)

            local close = main.createButton(1, 1, 5, 1, "close", function()
                window.remove()
                gui.redraw()
            end)
            close.backColor = gui.selectColor(0xFF0000, nil, false)
            close.foreColor = gui.selectColor(0xFFFFFF, nil, true)
            window.attachObj(1, 1, close)

            gui.redraw()
        end

        local refresh

        local list = main.createList(2, 2, main.sizeX - 2, main.sizeY - 4, function(str, button)
            if button == 1 and gui.context(gui.lastTouch[3], gui.lastTouch[4], {"remove", "cancel"}) == "remove" then
                local function table_remove(tbl, obj)
                    for i = 1, #tbl do
                        if tbl[i] == obj then
                            table.remove(tbl, i)
                        end
                    end
                end
                table_remove(lockCfg.users, str)
                refresh()
            end
        end)

        function refresh()
            list.clear()
            for i, name in ipairs(lockCfg.users) do
                list.addStr(name)
            end
        end
        refresh()

        local b1 = main.createInputbox(2, main.sizeY - 1, (main.sizeX // 2) - 2, 1, "add user", function(str)
            if su.inTable(lockCfg.users, str) then
                gui.splash("пользователь уже в списке")
            elseif str == "" then
                gui.splash("нельзя использовать пустое имя")
            else
                table.insert(lockCfg.users, str)
                refresh()
            end
        end)
        b1.viewData = false

        local b2 = main.createButton((main.sizeX // 2) + 1, main.sizeY - 1, (main.sizeX // 2) - 2, 1, "auto add", function(_, _, button, nik)
            if su.inTable(lockCfg.users, nik) then
                gui.splash("вы уже в списке")
            else
                table.insert(lockCfg.users, nik)
                refresh()
            end
        end)
        
        gui.attachExitCallback(function()
            saveCfg()
        end)
        gui.select(main)
        gui.run()
    end
end

local function setPasswordScreen()
    if --[[math.floor(computer.getDeviceInfo()[term.screen()].width) == 1 or]] options.s then
        term.clear()
        print("введите новый пароль")
        local read = io.read()
        if not read then term.clear() return end
        print("подтвердите новый пароль")
        local read2 = io.read()
        if not read2 then term.clear() return end

        if read == read2 then
            lockCfg.passwordSha256 = sha256(read)
            saveCfg()
        end
    else
        local gui = require("guix").create()
        
        local mx, my = 50, 16
        if component.isAvailable("tablet") then
            mx, my = gui.maxX, gui.maxY
        end

        local main = gui.createScene(gui.selectColor(0xFFFFFF, nil, false), mx, my)

        local x, y = main.getCenter()
        y = y - 2
        local i1 = main.createInputbox(x - 8, y, 16, 1, "enter password")
        i1.viewData = false
        i1.button.backColor = gui.selectColor(0xAAAAAA, nil, true)
        
        local x, y = main.getCenter()
        local i2 = main.createInputbox(x - 8, y, 16, 1, "commit password")
        i2.viewData = false
        i2.button.backColor = gui.selectColor(0xAAAAAA, nil, true)

        main.createTimer(0.05, function()
            i1.draw()
            i2.draw()
        end)

        local x, y = main.getCenter()
        y = y + 2
        local b1 = main.createButton(x - 8, y, 16, 1, "commit", function()
            if not i1.userInput or not i2.userInput then gui.splash("заполните все поля", gui.selectColor(0xFF0000, nil, false)) return end
            if i1.userInput ~= i2.userInput then
                gui.splash("пароли не совпадают", gui.selectColor(0xFF0000, nil, false))
                return
            end
            lockCfg.passwordSha256 = sha256(i1.userInput)
            saveCfg()
            gui.splash("настройки успешно устоновленны", gui.selectColor(0x00FF00, nil, false))
        end)
        b1.backColor = gui.selectColor(0xFFFF00, nil, true)
        b1.foreColor = gui.selectColor(0xAAAAAA, nil, false)

        gui.select(main)
        gui.run()
    end
end

local function setMainuserScreen()
    if --[[math.floor(computer.getDeviceInfo()[term.screen()].width) == 1 or]] options.s then
        term.clear()
        print("способ")
        print("1.ввести ник")
        print("2.устоновить меня")
        
        local num, nikname
        repeat
            local _, _, char, _, nikname2 = term.pull("key_down", term.keyboard())
            nikname = nikname2
            num = tonumber(string.char(math.floor(char)))
        until num == 1 or num == 2

        if num == 1 then
            local read = io.read()
            if not read then
                return
            end
            nikname = read
        end

        lockCfg.mainuser = nikname
        saveCfg()
        print("теперь " .. nikname .. " главный пользователь")
        os.sleep(2)
    else
        local gui = require("guix").create()

        local mx, my = 50, 16
        if component.isAvailable("tablet") then
            mx, my = gui.maxX, gui.maxY
        end

        local main = gui.createScene(gui.selectColor(0xFFFFFF, nil, false), mx, my)

        local function setNikname(nikname)
            lockCfg.mainuser = nikname
            saveCfg()
            gui.splash("теперь " .. nikname .. " главный пользователь", 0, nil, main.sizeX, main.sizeY)
        end

        local x, y = main.getCenter()
        y = y - 1
        local i1 = main.createInputbox(x - 8, y, 16, 1, "enter nikname", function(input)
            setNikname(input)
        end)
        i1.viewData = false
        i1.button.backColor = gui.selectColor(0xAAAAAA, nil, true)

        local x, y = main.getCenter()
        y = y + 1
        local b1 = main.createButton(x - 8, y, 16, 1, "auto", function(_, _, _, nikname)
            setNikname(nikname)
        end)
        b1.backColor = gui.selectColor(0xAAAAAA, nil, true)

        gui.select(main)
        gui.run()
    end
end

if args[1] == "set" then
    if args[2] == "password" then
        if not unlockScreen(true, false) then return end
        setPasswordScreen()
        term.clear()
    elseif args[2] == "mainuser" then
        if not unlockScreen(true, false) then return end
        setMainuserScreen()
        term.clear()
    elseif args[2] == "users" then
        if not unlockScreen(true, false) then return end
        usersMenager()
        term.clear()
    elseif args[2] == "on" then
        if not unlockScreen(true, false) then return end
        lockCfg.lock = true
        saveCfg()
        term.clear()
    elseif args[2] == "off" then
        if not unlockScreen(true, false) then return end
        lockCfg.lock = false
        saveCfg()
        term.clear()
    else
        print("нет такого параметра")
    end
elseif args[1] == "help" then
    print("lock [-a(do not power off allow)] [-c(check)] [-s(simple graphic)]")
    print("lock set password")
    print("lock set mainuser(его ник будет писаться в случаи ошибки для обшашения)")
    print("lock set users")
    print("lock set on")
    print("lock set off")
else
    local oldHookState = event.superHook
    event.superHook = false
    while true do
        local ok, err = pcall(unlockScreen, false, not options.a)
        if not ok then
            su.logTo("/free/logs/lockError.log", err or "unkown")
        else
            break
        end
    end
    event.superHook = oldHookState
    return true
end