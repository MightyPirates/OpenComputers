if computer.setArchitecture then pcall(computer.setArchitecture, "Lua 5.3") end --зашита от моих биосов(они усторели и удин удаляет setArchitecture а другой заставляет его выдать ошибку)

if _VERSION ~= "Lua 5.3" then
    error("requires Lua 5.3 pull out the processor and press shift plus the right mouse button on it", 0)
end

-----------------------------------mods

_G.startEepromAddress = component.list("eeprom")()

_G.origCoroutine = coroutine

computer.superRawPullSignal = computer.pullSignal
computer.superRawPushSignal = computer.pushSignal

do --прирывания
    local uptime = computer.uptime
    local oldInterruptTime = uptime()
    _G.interruptAtTime = 2
    _G.interruptTime = 0.2

    function _G.interrupt()
        if uptime() - oldInterruptTime > _G.interruptAtTime then
            os.sleep(_G.interruptTime) --на данный момент в _G его нет но он появиться после полной загрузки
            oldInterruptTime = uptime()
        end
    end
end

do --atan2 в Lua 5.3
    local atan = math.atan
    function math.atan2(y, x)
        return atan(y / x)
    end
end

--[[ --лагает, не ставьте клавы рядом
do --зашита от многократных нажатий кнопок
    local component = component
    local computer = computer
    local table = table
    local computer_pullSignal = computer.pullSignal
    local uptime = computer.uptime

    _G.keyboardsBlockTime = {}

    function computer.pullSignal(time)
        local term = require("term")
        local su = require("superUtiles")

        if not time then time = math.huge end
        local inTime = uptime()

        ::tonew::
        local eventData = {computer_pullSignal(time - (uptime() - inTime))}

        if eventData[2] and su.inTable(term.keyboards(), eventData[2]) then --системма работает только на основном мониторе, нада на стороньнем пилити сами я пас
            if keyboardsBlockTime[eventData[2] ] then
                if uptime() - keyboardsBlockTime[eventData[2] ] < 0.5 then
                    goto tonew
                end
            end
            for i, v in ipairs(term.keyboards()) do --накладывает delay на все клавиатуры
                if v ~= eventData[2] then --кроме той на которой был произведен ввод
                    keyboardsBlockTime[v] = uptime()
                end
            end
        end

        return table.unpack(eventData)
    end
end
]]

do --для таблиц в event
    local buffer = {}

    local oldPull = computer.pullSignal
    local oldPush = computer.pushSignal
    local tinsert = table.insert
    local tunpack = table.unpack
    local tremove = table.remove

    function computer.pullSignal(timeout)
        if #buffer == 0 then
            return oldPull(timeout)
        else
            local data = buffer[1]
            tremove(buffer, 1)
            return tunpack(data)
        end
    end

    function computer.pushSignal(...)
        tinsert(buffer, {...})
        return true
    end
end

do --спяший режим
    local computer = computer
    local computer_pullSignal = computer.pullSignal
    local computer_pushSignal = computer.pushSignal
    local computer_uptime = computer.uptime
    local table_unpack = table.unpack
    local checkArg = checkArg

    uptimeAdd = 0
    function computer.uptime()
        return computer_uptime() + uptimeAdd
    end

    function computer.sleep(time, saveEvent, doNotCorectUptime)
        checkArg(1, time, "number")
        checkArg(2, saveEvent, "nil", "boolean")
        checkArg(3, doNotCorectUptime, "nil", "boolean")
        local inTime = computer_uptime()
        while computer_uptime() - inTime < time do
            local eventData = {computer_pullSignal(time - (computer_uptime() - inTime))}
            if saveEvent and #eventData > 0 then
                computer_pushSignal(table_unpack(eventData))
            end
        end
        if not doNotCorectUptime then
            uptimeAdd = uptimeAdd - (computer_uptime() - inTime)
        end
    end

    function computer.delay(time)
        computer.sleep(time, true, true)
    end
end

if component.invoke(computer.getBootAddress(), "exists", "/free/flags/adminprotect") then --чтоб админ в комп заприваченый не тыкал
    local computer = computer
    local table = table
    local computer_pullSignal = computer.pullSignal

    _G.oldUptime = -math.huge

    function computer.pullSignal(time)
        if not time then time = math.huge end

        local term = require("term")
        if not term.isAvailable() then return computer_pullSignal(time) end

        local su = require("superUtiles")
        local frames = require("frames")
        local colorPic = require("colorPic")
        local colors = colorPic.getColors()

        local inTime = computer.uptime()

        ::ret::
        local waitTime = time - (computer.uptime() - inTime)
        if waitTime < 0 then return end
        local eventData = {computer_pullSignal(waitTime)}

        local users = {computer.users()}

        local function splash()
            if not term.isAvailable() or not _G.inited then return end
            if computer.uptime() - oldUptime > 5 then
                frames.splash(1, 5, 49, 3, "ADMIN PROTECT вкл, админ тоже должен быть добален", 2, su.selectColor(nil, colors.red, nil, false), su.selectColor(nil, colors.orange, nil, true))
                oldUptime = computer.uptime()
            end
        end

        if (eventData[1] == "touch" or eventData[1] == "drag" or eventData[1] == "drop" or eventData[1] == "scroll") and #users > 0 and not su.inTable(users, eventData[6]) then
            splash()
            goto ret
        end

        if (eventData[1] == "key_up" or eventData[1] == "key_down") and #users > 0 and not su.inTable(users, eventData[5]) then
            splash()
            goto ret
        end

        if (eventData[1] == "clipboard") and #users > 0 and not su.inTable(users, eventData[4]) then
            splash()
            goto ret
        end

        return table.unpack(eventData)
    end
end

do --для запуска древнючего софта
    local component = component
    local computer = computer
    function computer.isRobot()
        return component.isAvailable("robot")
    end
end

computer.rawPullSignal = computer.pullSignal

-----------------------------------

do --активатор загрузчика
    local addr, invoke = computer.getBootAddress(), component.invoke
    local function loadfile(file)
        local handle = assert(invoke(addr, "open", file))
        local buffer = ""
        repeat
            local data = invoke(addr, "read", handle, math.huge)
            buffer = buffer .. (data or "")
        until not data
        invoke(addr, "close", handle)
        return load(buffer, "=" .. file, "bt", _G)
    end
    if not _G.recoveryMod then
        local path = "/free/twicks/mem/"
        local tbl = component.proxy(computer.getBootAddress()).list(path) or {}
        table.sort(tbl)
        for i, v in ipairs(tbl) do
            local full_path = path .. v
            loadfile(full_path)()
        end
    end
    loadfile("/lib/core/boot.lua")(loadfile)
end

-----------------------------------

local fs = require("filesystem")
local term = require("term")
local event = require("event")
local component = require("component")
local computer = require("computer")
local su = require("superUtiles")

event.listen("init", function()
    _G.inited = true
    return false
end)

do --оптимизация computer.getDeviceInfo
    local computer_getDeviceInfo = computer.getDeviceInfo
    local deviveinfo

    function computer.getDeviceInfo()
        return deviveinfo
    end

    function computer.refreshDeviveInfo()
        deviveinfo = computer_getDeviceInfo()
    end
    computer.refreshDeviveInfo()

    event.listen("component_added", computer.refreshDeviveInfo)
    event.listen("component_removed", computer.refreshDeviveInfo)
end

-----------------------------------

local autorunspath = "/autoruns" --блок упровления автозагрузкой
local systemautoruns = fs.concat(autorunspath, "system")
local userautoruns = fs.concat(autorunspath, "user")
local afterBootTwicks = "/free/twicks/afterBoot1"

local function list(path)
    local tbl = fs.get(path).list(path)
    table.sort(tbl)
    return ipairs(tbl)
end

-----------------------------------

fs.makeDirectory("/free/flags")
fs.makeDirectory("/usr/bin")
fs.makeDirectory("/usr/lib")
fs.makeDirectory("/autoruns/user")

if fs.exists(afterBootTwicks) and not _G.recoveryMod then --запуск boot твиков после запуска класической openOS
    for _, data in list(afterBootTwicks) do
        os.execute(fs.concat(afterBootTwicks, data))
    end
end

-----------------------------------

if fs.exists(systemautoruns) then --системная автозагрузка
    for _, data in list(systemautoruns) do
        local ok, err = xdofile(fs.concat(systemautoruns, data))
        if not ok then
            su.logTo("/free/logs/systemAutorunsErrors.log", (err or "unkown") .. "\n")
        end
    end
end

if fs.exists("/free/flags/updateEnd") and not _G.recoveryMod then --запуска файла дополнения обновления(для оболочек)
    local afterUpdate = false
    if fs.exists("/afterUpdate.lua") then
        local ok, err = sdofile("/afterUpdate.lua")
        if not ok then
            su.logTo("/free/logs/afterUpdateError.log", err or "unkown")
            computer.shutdown("fast")
            return
        end
        afterUpdate = true
    end
    fs.remove("/free/flags/updateEnd")
    if afterUpdate then
        computer.shutdown("fast")
        return
    end
end

-----------------------------------экран блокировки

os.execute("lock -c")

-----------------------------------

_G.runlevel = 1
event.push("init") --подтверждает инициализацию системмы
event.pull(1, "init")

-----------------------------------

_G.filesystemsInit = true --разришить автозогрузку с внешних насителей
if not _G.recoveryMod then
    for address in component.list("filesystem") do
        event.push("component_added", address, "filesystem") --инициирует файловых системм
    end
    for i = 1, 2 do os.sleep(0.2) end
end

local shell = require("shell")
local package = require("package")

shell.setPath(shell.getPath() .. ":/tmp/bin")
shell.setPath(shell.getPath() .. ":/tmp/usr/bin")
shell.setPath(shell.getPath() .. ":/tmp/home/bin")

package.path = package.path .. ";/tmp/lib/?.lua"
package.path = package.path .. ";/tmp/lib/?/init.lua"
package.path = package.path .. ";/tmp/usr/lib/?.lua"
package.path = package.path .. ";/tmp/usr/lib/?/init.lua"
package.path = package.path .. ";/tmp/home/lib/?.lua"
package.path = package.path .. ";/tmp/home/lib/?/init.lua"

-----------------------------------

if fs.exists(userautoruns) and not _G.recoveryMod then --автозагрузка пользователя
    for _, data in list(userautoruns) do
        os.execute(fs.concat(userautoruns, data))
    end
end

-----------------------------------

if not _G.recoveryMod then
    if fs.exists("/.start.lua") then --главная автозагрузка
        os.execute("/.start.lua")
    elseif fs.exists("/.autorun.lua") then
        os.execute("/.autorun.lua")
    end

    if fs.exists("/autorun.lua") then os.execute("/autorun.lua") end
    if fs.exists("/start.lua") then os.execute("/start.lua") end
end

-----------------------------------

local function waitFoEnter()
    os.sleep(0.5)
    while true do
        local _, uuid, _, code = event.pull("key_down")
        if term.keyboard() and su.inTable(term.keyboards(), uuid) and code == 28 then
            break
        end
    end
end

event.push("full_load")
_G.full_load = true
while _G.shellAllow or _G.recoveryMod do --запуск shell
    local result, reason = xpcall(require("shell").getShell(), function(msg)
        return tostring(msg) .. "\n" .. debug.traceback()
    end)
    if not result then
        computer.pullSignal() --для возможности крашнуть комп с ошибкой
        if term.isAvailable() then
            io.stderr:write((reason ~= nil and tostring(reason) or "unknown error") .. "\n")
            io.write("Press enter key to continue.\n")
            waitFoEnter()
        end
    end
end

if term.isAvailable() then
    term.gpu().setBackground(0)
    term.gpu().setForeground(0xFFFFFF)
    io.write("Shell is not allow, press enter key to reboot.\n")
    waitFoEnter()
end
computer.shutdown("fast")