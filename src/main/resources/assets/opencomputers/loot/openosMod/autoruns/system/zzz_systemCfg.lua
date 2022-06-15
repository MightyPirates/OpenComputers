local serialization = require("serialization")
local su = require("superUtiles")
local fs = require("filesystem")
local event = require("event")
local component = require("component")
local term = require("term")
local computer = require("computer")
local shell = require("shell")
local uuid = require("uuid")
local colorPic = require("colorPic")
local cryptoData = require("cryptoData")
local frames = require("frames")

local colors = colorPic.getColors()

if term.isAvailable() then
    local function depthFilter(address, func, _, ...)
        local depth = ...
        event.push("depthChanged", address, depth)
        return func(...)
    end
    local function setFilter()
        cryptoData.addFilterMethod(term.gpu().address, "setDepth", depthFilter)
    end
    setFilter()
    event.listen("term_available", function()
        setFilter()
    end)
end

------------------------------------

for address in component.list("modem") do
    component.invoke(address, "close") --close all ports
    if component.invoke(address, "isWireless") then
        component.invoke(address, "setStrength", math.huge) --set max strength
    end
end

_G.readonly = fs.get("/").isReadOnly()

if not fs.exists("/free/unical/systemUuid") then
    su.saveFile("/free/unical/systemUuid", uuid.next())
end

local function getType(checkType)
    local _, c = component.list(checkType)()
    return c
end

if not fs.exists("/free/unical/deviceType") then
    su.saveFile("/free/unical/deviceType", getType("tablet") or getType("robot") or getType("drone") or getType("microcontroller") or "computer")
end

if not fs.exists("/free/unical/deviceAddress") then
    su.saveFile("/free/unical/deviceAddress", computer.address())
end

if not fs.exists("/free/unical/fsAddress") then
    su.saveFile("/free/unical/fsAddress", fs.get("/").address)
end

if not fs.exists("/free/unical/startEepromAddress") then
    su.saveFile("/free/unical/startEepromAddress", _G.startEepromAddress or "nil")
end

su.saveFile("/free/current/systemUuid", uuid.next())
su.saveFile("/free/current/deviceAddress", computer.address())
su.saveFile("/free/current/deviceType", getType("tablet") or getType("robot") or getType("drone") or getType("microcontroller") or "computer")
su.saveFile("/free/current/fsAddress", fs.get("/").address)
su.saveFile("/free/current/startEepromAddress", _G.startEepromAddress or "nil")

------------------------------------

if not _G.recoveryMod then
    if fs.exists("/free/flags/error") then
        shell.execute("error", nil, su.getFile("/free/flags/error"))
    elseif fs.get("/").isReadOnly() then
        --os.execute("error \"drive is readonly\"")
    end
end

------------------------------------

local function updateValue(path)
    if fs.exists(path) then
        local count = tonumber(su.getFile(path))
        count = count + 1
        su.saveFile(path, tostring(count))
    else
        su.saveFile(path, "1")
    end
end

updateValue("/free/data/powerOnCount")

if fs.exists("/free/flags/powerOn") then
    updateValue("/free/data/powerWarning")
else
    su.saveFile("/free/flags/powerOn", "")
end
event.listen("shutdown", function()
    fs.remove("/free/flags/powerOn")
    updateValue("/free/data/likePowerOffCount")
end)

------------------------------------

local function createSystemCfg()
    return {updateErrorScreen = true, lowPowerSplash = true, lowPowerSound = true, lowPowerOffSplash = true, lowPowerOffSound = true, superHook = true, hook = true, shellAllow = true, autoupdate = false, updateRepo = "https://raw.githubusercontent.com/igorkll/openOSpath/main", updateVersionCfg = "/version.cfg", logo = true, startSound = true}
end

local created = createSystemCfg()

function _G.saveSystemConfig()
    su.saveFile("/etc/system.cfg", serialization.serialize(_G.systemCfg or created))
end

function _G.loadSystemConfig()
    if fs.exists("/etc/system.cfg") then
        _G.systemCfg = assert(serialization.unserialize(assert(su.getFile("/etc/system.cfg"))))
    else
        _G.systemCfg = created
    end
end

if not fs.exists("/etc/system.cfg") then saveSystemConfig() end

_G.loadSystemConfig()

if _G.recoveryMod then
    _G.systemCfg = created
else
    local oldTable = serialization.serialize(_G.systemCfg)
    for k, v in pairs(created) do
        if _G.systemCfg[k] == nil then
            _G.systemCfg[k] = v
        end
    end
    if serialization.serialize(_G.systemCfg) ~= oldTable then
        _G.saveSystemConfig()
    end
end

------------------------------------

function _G.getEnergyPercentages()
    return math.floor(su.mapClip(computer.energy(), 0, computer.maxEnergy(), 0, 100) + 0.5)
end

if not _G.recoveryMod then
    local shutdownPart = 8

    local computer_energy = computer.energy
    local computer_maxEnergy = computer.maxEnergy
    function computer.energy()
        return su.mapClip(computer_energy(), computer_maxEnergy() / shutdownPart, computer_maxEnergy(), 0, computer_maxEnergy())
    end
    function computer.maxEnergy()
        return computer_maxEnergy() - (computer_maxEnergy() / shutdownPart)
    end

    function _G.lowPowerDraw()
        if term.isAvailable() then
            local targetPath = "/system/images/lowPower.pic"
            if math.floor(term.gpu().getDepth()) == 1 then
                targetPath = "/system/images/lowPowerBW.pic"
            end

            if fs.exists(targetPath) then
                local imageDrawer = require("imageDrawer")
                local img = imageDrawer.loadimage(targetPath)
                term.clear()

                local ix, iy = img.getSize()
                local rx, ry = term.gpu().getResolution()
                local cx, cy = rx / 2, ry / 2
                local dx, dy = math.floor((cx - (ix / 2)) + 0.5), math.floor((cy - (iy / 2)) + 0.5)
                img.draw(dx, dy)
                return true
            end
        end
    end

    local timerID
    local oldtime = -math.huge

    local function check()
        local pow = getEnergyPercentages()
        if pow <= 20 and _G.full_load then
            if computer.uptime() - oldtime > ((pow <= 10) and ((pow <= 3) and 1 or 5) or 25) then
                if systemCfg.lowPowerSound then computer.beep(100, 0.5) end
                if systemCfg.lowPowerSplash and term.isAvailable() then
                    frames.splash(1, 5, 30, 3, "низкий заряд батареи " .. tostring(pow) .. "%", 1, su.selectColor(nil, colors.red, nil, false), su.selectColor(nil, colors.orange, nil, true))
                end
                oldtime = computer.uptime()
            end
        end
        if pow <= 0 then
            if timerID then
                event.cancel(timerID)
            end
            local isDelay
            if systemCfg.lowPowerOffSplash then isDelay = lowPowerDraw() end
            if systemCfg.lowPowerOffSound then
                computer.beep(100, 0.1)
                computer.beep(80, 0.1)
                computer.beep(50, 0.5)
            end
            if isDelay then computer.delay(2) end
            computer.shutdown()
        end
    end
    check()
    timerID = event.timer(1, check, math.huge)
end

function _G.updateAllow()
    return _G.getEnergyPercentages() >= 50
end

------------------------------------

function _G.updateNoInternetScreen()
    event.superHook = false
    if not term.isAvailable() or not _G.systemCfg.updateErrorScreen then computer.shutdown("fast") end

    local rx, ry = su.getTargetResolution()

    local gui = require("simpleGui2").create(rx, ry)
    local color = 0x6699FF

    gui.status("при предидушем обновлениия произошла ошибка", 0xFFFFFF, color)
    os.sleep(2)
    gui.status("подлючите internet card, чтобы все исправить", 0xFFFFFF, color)
    os.sleep(2)
    gui.status("убедитесь что реальный пк подключен к интернету", 0xFFFFFF, color)
    os.sleep(2)
    computer.shutdown("fast")
end

local function updateLowPowerScreen()
    event.superHook = false
    if not term.isAvailable() or not _G.systemCfg.updateErrorScreen then computer.shutdown() end

    local rx, ry = su.getTargetResolution()

    local gui = require("simpleGui2").create(rx, ry)
    local color = 0x6699FF

    gui.status("при предидушем обновлениия произошла ошибка", 0xFFFFFF, color)
    os.sleep(2)
    gui.status("для повторной попытки обновления мало энергии", 0xFFFFFF, color)
    os.sleep(2)
    gui.status("необходимо хотябы 50% у вас " .. tostring(getEnergyPercentages()) .. "%", 0xFFFFFF, color)
    os.sleep(2)
    computer.shutdown("fast")
end

------------------------------------

--os.execute("lock -c")

local function drawLogo()
    if _G.systemCfg.logo and term.isAvailable() then
        local img
        local gpu = term.gpu()
        local rx, ry = gpu.getResolution()
        if math.floor(gpu.getDepth()) ~= 1 then
            gpu.setBackground(su.selectColor(nil, colors.lightGray, nil, false))
            gpu.setForeground(0xFFFFFF)
            gpu.fill(1, 1, rx, ry, " ")
            if fs.exists("/system/images/logo.pic") then
                img = require("imageDrawer").loadimage("/system/images/logo.pic")
            elseif fs.exists("/system/images/logoBW.pic") then
                img = require("imageDrawer").loadimage("/system/images/logoBW.pic")
            end
        else
            gpu.setBackground(0)
            gpu.setForeground(0xFFFFFF)
            gpu.fill(1, 1, rx, ry, "▒")

            if fs.exists("/system/images/logoBW.pic") then
                img = require("imageDrawer").loadimage("/system/images/logoBW.pic")
            elseif fs.exists("/system/images/logo.pic") then
                img = require("imageDrawer").loadimage("/system/images/logo.pic")
            end
        end
        if img then
            local rx, ry = gpu.getResolution()
            local cx, cy = img.getSize()
            cx, cy = (rx / 2) - (cx / 2), (ry / 2) - (cy / 2)
            cx = math.floor(cx) + 1
            cy = math.floor(cy) + 1
            img.draw(cx, cy)
        end
    end
end

_G.updateRepo = systemCfg.updateRepo

if not _G.recoveryMod and not _G.readonly then
    local isInternet = su.isInternet()
    if systemCfg.autoupdate or fs.exists("/free/flags/updateStart") then
        if fs.exists("/free/flags/updateStart") then
            if isInternet then
                if _G.updateAllow() then
                    os.execute("fastupdate -f")
                else
                    updateLowPowerScreen()
                end
            else
                _G.updateNoInternetScreen()
            end
        else
            if isInternet then
                os.execute("fastupdate")
            end
        end
    end
end

drawLogo()

event.superHook = systemCfg.superHook
event.hook = systemCfg.hook
_G.shellAllow = systemCfg.shellAllow

if _G.systemCfg.startSound then
    if fs.exists("/etc/startSound.mid") then
        local function beep(n, d)
            if component.isAvailable("beep") then
                component.beep.beep({[n] = d})
            else
                computer.beep(n, d)
            end
        end
        require("midi2").create("/etc/startSound.mid", {beep}).play()
    else
        computer.beep(2000, 0.5)
        for i = 1, 4 do
            computer.beep(500, 0.01)
        end
        computer.beep(1000, 1)
    end
elseif term.isAvailable() and _G.systemCfg.logo then
    os.sleep(2)
end
--[[
if term.isAvailable() then
    term.gpu().setBackground(0)
    term.gpu().setForeground(0xFFFFFF)
    term.clear()
end
]]

if fs.exists("/etc/runCommand.dat") then os.execute(su.getFile("/etc/runCommand.dat")) end