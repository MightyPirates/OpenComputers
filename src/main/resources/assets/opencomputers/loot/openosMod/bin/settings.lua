local gui = require("simpleGui3").create()
local fs = require("filesystem")
local su = require("superUtiles")
local computer = require("computer")
local twicks = require("twicks")
local event = require("event")

---------------------------------------------

local function resetSettings()
    for i, v in ipairs(twicks.twicks()) do --отключаем все твики
        pcall(twicks.disable, v)
    end

    fs.remove("/usr/bin")
    fs.remove("/usr/lib")
    fs.remove("/start.lua")
    fs.remove("/autorun.lua")
    fs.remove("/autoruns/user")
    fs.remove("/home")
    fs.remove("/free")
    fs.remove("/afterUpdate.lua")
    fs.remove("/beforeUpdate.lua")

    su.saveFile("/home/.shrc", "")

    local bl = {
    "/etc/motd",
    "/etc/screen.cfg", --screen хоть и пользовательская настройка но сбивать я ее не хочу
    "/etc/rc.d",
    "/etc/rc.cfg",
    "/etc/profile.lua",
    }

    for file in fs.list("/etc") do
        local full_path = fs.concat("/etc", file)
        if not su.inTable(bl, full_path) then
            fs.remove(full_path)
        end
    end
end

---------------------------------------------

local num
while true do
    num = gui.menu("Settings", {
    "сброс на заводские настройки",
    "автозагрузка с внешних насителей",
    "автообновления",
    "логотип при загрузке",
    "звук при загрузки",
    "ctrl+atl+c прирывания",
    "ctrl+c прирывания",
    "admin protect & reboot",
    "выбрать канал обновлений",
    "выход"},
    num)
    if num == 1 then
        gui.status([[
вы уверены что хотите сбросить устройство?
вы потеряете все свои данные и программы
все настройки также собьються
это поможет очистить устройство от вирусов
однако не от всех
так же помните что сбрасываються только данных
пользователя все изменения внесенные в ос
не будут сброшены]], true)
        if gui.yesno("вы уверены произвести сброс настроек?") then
            resetSettings()
            computer.shutdown(true)
        end
    elseif num == 2 then
        local newstate = gui.yesno("внешнея автозагрузки", true, fs.isAutorunEnabled() and 2 or 1)
        fs.setAutorunEnabled(newstate)
    elseif num == 3 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        cfg.autoupdate = gui.yesno("автообновления", true, cfg.autoupdate and 2 or 1)
        _G.systemCfg.autoupdate = cfg.autoupdate
        assert(su.saveTable("/etc/system.cfg", cfg))
    elseif num == 4 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        cfg.logo = gui.yesno("логотип при загрузке", true, cfg.logo and 2 or 1)
        _G.systemCfg.logo = cfg.logo
        assert(su.saveTable("/etc/system.cfg", cfg))
    elseif num == 5 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        cfg.startSound = gui.yesno("звук при загрузки", true, cfg.startSound and 2 or 1)
        _G.systemCfg.startSound = cfg.startSound
        assert(su.saveTable("/etc/system.cfg", cfg))
    elseif num == 6 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        cfg.superHook = gui.yesno("ctrl+atl+c прирывания", true, cfg.superHook and 2 or 1)
        assert(su.saveTable("/etc/system.cfg", cfg))
        event.superHook = cfg.superHook
    elseif num == 7 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        cfg.hook = gui.yesno("ctrl+c прирывания", true, cfg.hook and 2 or 1)
        assert(su.saveTable("/etc/system.cfg", cfg))
        event.hook = cfg.hook
    elseif num == 8 then
        local oldstate = fs.exists("/free/flags/adminprotect")
        local newstate = gui.yesno("admin protect не даст админу залезть в комп", true, oldstate and 2 or 1)
        if newstate ~= oldstate then
            if newstate then
                su.saveFile("/free/flags/adminprotect", "")
            else
                fs.remove("/free/flags/adminprotect")
            end
            computer.shutdown(true)
        end
    elseif num == 9 then
        local cfg = assert(su.getTable("/etc/system.cfg"))
        local channel
        local channelNum
        if cfg.updateRepo == "https://raw.githubusercontent.com/igorkll/openOSpath/main" then
            channel = "main"
            channelNum = 1
        elseif cfg.updateRepo == "https://raw.githubusercontent.com/igorkll/openOSpath/dev" then
            channel = "dev"
            channelNum = 2
        end

        local num = gui.menu("ваш канал обновлений: " .. (channel or "custom"), {"main(рекомендуеться)", "dev(тестовый канал, может быть нестабилен)"}, channelNum)
        if num ~= channelNum then
            if num == 1 then
                cfg.updateRepo = "https://raw.githubusercontent.com/igorkll/openOSpath/main"
            elseif num == 2 then
                cfg.updateRepo = "https://raw.githubusercontent.com/igorkll/openOSpath/dev"
            end
            _G.systemCfg.updateRepo = cfg.updateRepo
            assert(su.saveTable("/etc/system.cfg", cfg))
            os.execute("fastupdate -f")
        end
    elseif num == 10 then
        gui.exit()
    end
end