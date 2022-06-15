local gui = require("simpleGui3").create()
local su = require("superUtiles")
local fs = require("filesystem")
local computer = require("computer")
local twicks = require("twicks")

-------------------------------------------------

local function getError(err)
    return err or "unknown error"
end

-------------------------------------------------

local function control(state)
    local num
    while true do
        local names = twicks.twicks(state)

        local strs = {}
        for i, v in ipairs(names) do
            table.insert(strs, v .. ", state: " .. tostring(twicks.isEnabled(v)))
        end
        table.insert(strs, "back")

        num = gui.menu("twicks", strs, num)
        local name = names[num]
        if not name then
            return
        end

        local num2
        while true do
            num2 = gui.menu("control twick: " .. name .. ", state: " .. tostring(twicks.isEnabled(name)), {(twicks.isEnabled(name) and "disable" or "enable") .. " & reboot", "remove" .. (twicks.isEnabled(name) and " & reboot" or ""), "back"}, num2)
            if num2 == 1 then
                if twicks.changeState(name) then
                    computer.shutdown(true)
                end
            elseif num2 == 2 then
                if twicks.remove(name) then
                    computer.shutdown(true)
                end
                break
            elseif num2 == 3 then
                break
            end
        end
    end
end

local function market()
    if not su.isInternet() then
        gui.status("internet error", true)
        return
    end
    local data = assert(su.getInternetFile("https://raw.githubusercontent.com/igorkll/twicksForMod/main/twlist.txt"))
    local splitdata = su.split(data, "\n")

    local links = {}
    local names = {}
    for i, v in ipairs(splitdata) do
        local link, name = table.unpack(su.split(v, ";"))
        table.insert(links, link)
        table.insert(names, name)
    end
    table.insert(names, "back")

    local num
    while true do
        num = gui.menu("market", names, num)
        local name = names[num]
        local link = links[num]
        if not link then return end

        if twicks.isInstalled(name) then
            gui.status("this twick installed", true)
        else
            local num2 = gui.menu("download twick " .. name .. "?", {"download", "back"})
            if num2 == 1 then
                local data = assert(su.getInternetFile(link))
                twicks.insert(name, data)
            end
        end
    end
end

-------------------------------------------------

local num
while true do
    num = gui.menu("twick menager", {"downloaded", "market", "enabled", "disabled", "back"}, num)
    if num == 1 then
        control()
    elseif num == 2 then
        market()
    elseif num == 3 then
        control(true)
    elseif num == 4 then
        control(false)
    elseif num == 5 then
        gui.exit()
    end
end