local gui = require("guix").create()
local su = require("superUtiles")
local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)

--------------------------------------------

local cleanUrl = "https://raw.githubusercontent.com/igorkll/appMarket3/main"

if su.isInternet() and not options.l then
    appsList = su.getInternetFile(cleanUrl .. "/list.txt")
    if appsList then
        appsList = su.split(appsList, "\n")
    end
end

--------------------------------------------

local function setLocalVersion(name, version)
    assert(su.saveFile(fs.concat("/free/programs/versions", name), tostring(version)))
end

local function getLocalVersion(name)
    if not fs.exists(fs.concat("/free/programs/versions", name)) then return nil end
    return assert(tonumber(assert(su.getFile(fs.concat("/free/programs/versions", name)))))
end

local function resetLocalVersion(name)
    fs.remove(fs.concat("/free/programs/versions", name))
end

--------------------------------------------

connectScene = gui.createScene(gui.selectColor(0x222222, nil, false), gui.userX, gui.userY)

connectScene.createLabel(2, 2, (connectScene.sizeX // 2) - 2, 1, "internet" .. (not appsList and " (no internet found)" or ""))
connectScene.createLabel((connectScene.sizeX // 2) + 1, 2, (connectScene.sizeX // 2) - 1, 1, "installed")

if appsList then
    local urls
    local names
    local updateFlag
    local versions
    internetList = connectScene.createList(2, 4, (connectScene.sizeX // 2) - 2, connectScene.sizeY - 4, function(str, button, num)
        if not updateFlag[num] and fs.exists(fs.concat("/free/programs/menagers", names[num])) then gui.splash("this program is already installed") return end
        if gui.yesno((updateFlag[num] and "update " or "install ") .. names[num] .. "?") then
            gui.status(updateFlag[num] and "updating" or "installing")

            local url = urls[num]
            local data = assert(su.getInternetFile(url))
            assert(su.saveFile(fs.concat("/free/programs/menagers", names[num]), data))
            setLocalVersion(names[num], versions[num])

            gui.stop()
            shell.execute(fs.concat("/free/programs/menagers", names[num]), nil, "install")
            gui.start()
            gui.redraw()
            refreshInstalled()
            refreshInternetList()
        end
    end)

    function refreshInternetList()
        urls = {}
        names = {}
        updateFlag = {}
        versions = {}

        internetList.clear()
        for i, v in ipairs(appsList) do
            local url, name, version = table.unpack(su.split(v, ";"))
            version = tonumber(version)

            table.insert(urls, url)
            table.insert(names, name)
            table.insert(versions, version or false)

            if fs.exists(fs.concat("/free/programs/menagers", name)) and version and (not getLocalVersion(name) or version > getLocalVersion(name)) then
                internetList.addStr(name .. " (updata, current: " .. tostring(getLocalVersion(name)) .. ", new: " .. tostring(version) .. ")")
                table.insert(updateFlag, true)
            else
                internetList.addStr(name)
                table.insert(updateFlag, false)
            end
        end
    end
    refreshInternetList()
end

installedList = connectScene.createList((connectScene.sizeX // 2) + 1, 4, (connectScene.sizeX // 2) - 1, connectScene.sizeY - 4, function(str, button, num)
    local function command(com)
        gui.stop()
        shell.execute(fs.concat("/free/programs/menagers", str), nil, com)
        gui.start()
        gui.redraw()
    end
    if button == 1 then
        local selected = gui.context(gui.lastTouch[3], gui.lastTouch[4], {"open", "uninstall"})
        if selected == "open" then
            gui.status("loading")
            command("open")
        elseif selected == "uninstall" then
            gui.status("uninstalling")
            resetLocalVersion(str)
            command("uninstall")
            refreshInstalled()
        end
    elseif button == 0 then
        gui.status("loading")
        command("open")
    end
end)

function refreshInstalled()
    installedList.clear()
    for file in fs.list("/free/programs/menagers") do
        installedList.addStr(file)
    end
end
refreshInstalled()

--------------------------------------------

gui.select(connectScene)
gui.run()