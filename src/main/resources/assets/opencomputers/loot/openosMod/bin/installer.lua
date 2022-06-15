local component = component or require("component")
local computer = computer or require("computer")
local unicode = unicode or require("unicode")
local event = require("event")
local term = require("term")
local fs = require("filesystem") 

------------------------------------------------------init

if fs.get("/").isReadOnly() then error("drive is readonly") return end
if component.screen.setPrecise then pcall(component.screen.setPrecise, false) end

local function isAvailable(ctype)
    return not not component.list(ctype)()
end

local gpu = isAvailable("gpu") and component.gpu
local internet = isAvailable("internet") and component.internet
local eeprom = isAvailable("eeprom") and component.eeprom
local beep = computer.beep or function() end

if not gpu then error("gpu") return end
if not internet then error("internet card not found") return end

local depth = math.floor(gpu.getDepth())
local rx, ry = gpu.getResolution()

------------------------------------------------------main

local superMainurl = "https://raw.githubusercontent.com/igorkll/openOSpath"
local mainurl = "https://raw.githubusercontent.com/igorkll/openOSpath/main"
local label = "openOS Mod Installer V2"

local function getFile(path)
    local file, err = io.open(path, "rb")
    if not file then return nil, err end
    local data = file:read("*a")
    file:close()
    return data
end

local function saveFile(path, data)
    fs.makeDirectory(fs.path(path))
    local file, err = io.open(path, "wb")
    if not file then return nil, err end
    file:write(data)
    file:close()
    return true
end

local function split(str, sep)
    local parts, count = {}, 1
    local i = 1
    while true do
        if i > #str then break end
        local char = str:sub(i, #sep + (i - 1))
        if not parts[count] then parts[count] = "" end
        if char == sep then
            count = count + 1
            i = i + #sep
        else
            parts[count] = parts[count] .. str:sub(i, i)
            i = i + 1
        end
    end
    if str:sub(#str - (#sep - 1), #str) == sep then table.insert(parts, "") end
    return parts
end

local function wget(url)
    local handle, data, result, reason = internet.request(url), ""
    if handle then
        while 1 do
            result, reason = handle.read(math.huge)	
            if result then
                data = data .. result
            else
                handle.close()
                
                if reason then
                    return nil, reason
                else
                    return data
                end
            end
        end
    else
        return nil, "Unvalid Address"
    end
end
if not wget(mainurl .. "/null") then error("internet check error") return end

local function getList(url)
    local data = assert(wget(url))
    return split(data, "\n")
end

local colorsIndex
if depth == 4 then
    colorsIndex = {0xFFFFFF, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xFEFE8C, 0x7FCC19, 0xFF8888, 0x4C4C4C, 0xAAAAAA, 0x3366CC, 0xFF33EE, 0x333999, 0xAA2222, 0x005500, 0xCC4C4C, 0x000000}
else
    colorsIndex = {0xFFFFFF, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xFEFE8C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C, 0x999999, 0x4C99B2, 0xB266E5, 0x3366CC, 0x9F664C, 0x57A64E, 0xCC4C4C, 0x000000}
end
local colors = {}
colors.white = colorsIndex[1]
colors.orange = colorsIndex[2]
colors.magenta = colorsIndex[3]
colors.lightBlue = colorsIndex[4]
colors.yellow = colorsIndex[5]
colors.lime = colorsIndex[6]
colors.pink = colorsIndex[7]
colors.gray = colorsIndex[8]
colors.lightGray = colorsIndex[9]
colors.cyan = colorsIndex[10]
colors.purple = colorsIndex[11]
colors.blue = colorsIndex[12]
colors.brown = colorsIndex[13]
colors.green = colorsIndex[14]
colors.red = colorsIndex[15]
colors.black = colorsIndex[16]

local function hue(h)
    local s, v = 255, 255
    h = h / 255
    s = s / 255
    v = v / 255

    local r, g, b

    local i = math.floor(h * 6);

    local f = h * 6 - i;
    local p = v * (1 - s);
    local q = v * (1 - f * s);
    local t = v * (1 - (1 - f) * s);

    i = math.floor(i % 6)

    if i == 0 then
        r, g, b = v, t, p
    elseif i == 1 then
        r, g, b = q, v, p
    elseif i == 2 then
        r, g, b = p, v, t
    elseif i == 3 then
        r, g, b = p, q, v
    elseif i == 4 then
        r, g, b = t, p, v
    elseif i == 5 then
        r, g, b = v, p, q
    end

    r = math.floor(r * 255)
    g = math.floor(g * 255)
    b = math.floor(b * 255)

    if depth == 4 then
        local gray
        if r == g and g == b then
            gray = r
        end

        if gray then
            if gray <= 64 then
                gray = 0x000000
            elseif gray <= 128 then
                gray = 0x222222
            elseif gray <= 192 then
                gray = 0xAAAAAA
            else
                gray = 0xFFFFFF
            end
            return gray
        end

        local levelR, levelG, levelB = math.floor(r / 64), math.floor(g / 64), math.floor(b / 64)
        if (levelR >= 2) and (levelG ~= 0 and levelG ~= 3) and (levelB ~= 0 and levelB ~= 3) then
            return colors.pink
        elseif (levelR == 3) and (levelG >= 1 and levelG <= 2) and (levelB <= 1) then
            return colors.orange
        elseif (levelR == 0) and (levelG >= 2) and (levelB >= 1) then
            return colors.lightBlue
        end
    end
    return math.floor(b + (g * 256) + (r * 256 * 256))
end

local function selectColor(main, mini, bw)
    if depth == 4 then
        return mini or main
    elseif depth == 1 then
        return bw and 0xFFFFFF or 0x000000
    else
        return main
    end
end

local function map(value, low, high, low_2, high_2)
    local relative_value = (value - low) / (high - low)
    local scaled_value = low_2 + (high_2 - low_2) * relative_value
    return scaled_value
end

local status

local function install(url)
    status("install at url " .. url)
    os.sleep(1)
    local data = assert(wget(url .. "/filelist.txt"))
    for i, v in ipairs(split(data, "\n")) do
        status("downloading " .. v)
        local filedata, err = wget(url .. v)
        if not filedata then
            status("error to get file " .. err)
            os.sleep(0.5)
        end

        if filedata then
            saveFile(v, filedata)
        end
    end
end

------------------------------------------------------gui

local background = selectColor(0x666666, 0x222222, false)

local objs = {}
local function getPosFor(num)
    return (math.floor(ry / 2) - math.floor(#objs / 2)) + num
end

local function getPosX(text)
    return (math.floor(rx / 2) - math.floor(unicode.len(text) / 2)) + 1
end

local selectButton = 1

local function setText(text, posY, sizeX)
    local posX = getPosX(sizeX and string.rep(" ", sizeX) or text)
    gpu.set(posX, posY, text)
    return posX
end

local function drawAll()
    for i, v in ipairs(objs) do
        local posY = getPosFor(i)
        gpu.setBackground(background)
        gpu.fill(1, posY, rx, posY, " ")
        v.draw(posY)
    end
end

local pointPos
local function checkUsed(eventData)
    if eventData[1] == "key_down" then
        local posY = getPosFor(selectButton)
        if eventData[4] == 28 or eventData[4] == 57 then
            objs[selectButton].use()
        end
    elseif eventData[1] == "touch" then
        for i, v in ipairs(objs) do
            local posY = getPosFor(i)
            if math.floor(eventData[4]) == posY and (v.start and (math.floor(eventData[3]) >= v.start and math.floor(eventData[3]) <= v.stop) or math.floor(eventData[3]) == pointPos) then
                v.use()
            end
        end
    end
end

local function clearAll()
    while true do
        if #objs == 0 then break end
        table.remove(objs, 1)
    end
end

local function setPoint(posX, posY, state, selected)
    if selected then
        gpu.setBackground(state and 0x00FF00 or 0xFF0000)
        gpu.setForeground(background)
    else
        gpu.setForeground(background)
        gpu.setForeground(state and 0x00FF00 or 0xFF0000)
    end
    gpu.set(posX, posY, state and "√" or "╳")
end

local states = {_G._OSVERSION ~= "OpenOS 1.7.5", not _G.smartEfi, not _G._MODVERSION}
local active = {}
local names = {"обновить openOS до версии 1.7.5", "устоновить micro bios", "устоновить мод для openOS"}

if not eeprom then
    table.remove(names, 2)
    table.remove(states, 2)
end

local maxStrLen = 0
for i, v in ipairs(names) do
    if unicode.len(v) > maxStrLen then maxStrLen = unicode.len(v) end
end
pointPos = maxStrLen - 1
pointPos = (math.floor(rx / 2) - math.floor(pointPos / 2)) - 1

local function buttonUp()
    if selectButton > 1 then selectButton = selectButton - 1 end
end

local function buttonDown()
    if selectButton < #objs then selectButton = selectButton + 1 end
end

local function addButton(text, state, num)
    table.insert(active, state)

    local obj = {}
    obj.state = state or false

    function obj.draw(posY)
        gpu.setBackground(background)
        gpu.setForeground(0xFFFFFF)
        setText(text, posY, maxStrLen - 1)
        setPoint(pointPos, posY, obj.state, selectButton == num)
    end

    function obj.use()
        obj.state = not obj.state
        active[num] = obj.state
    end

    table.insert(objs, obj)
    return obj
end

local num
for i, v in ipairs(names) do
    addButton(v, states[i], i)
    num = i
end

local installers = {function() --updata
    install("https://raw.githubusercontent.com/igorkll/openOS/main")
end, function() --efi
    status("downloading bios")
    local biosCode = assert(wget("https://raw.githubusercontent.com/igorkll/topBiosV5/main/microBios.bin"))
    
    status("installing bios")
    eeprom.setData(fs.get("/").address .. "\n\n" .. "/init.lua")
    eeprom.set(biosCode)
    eeprom.setLabel("micro bios")
end, function() --install mod
    install(mainurl)
    status("saving config file")
    saveFile("/autoruns/user/set.lua", "local mainurl = \"" .. mainurl .. "\"\n" .. [[
local fs = require("filesystem")
local computer = computer or require("computer")
local serialization = require("serialization")

local function getFile(path)
    local file, err = io.open(path, "rb")
    if not file then return nil, err end
    local data = file:read("*a")
    file:close()
    return data
end

local function saveFile(path, data)
    fs.makeDirectory(fs.path(path))
    local file, err = io.open(path, "wb")
    if not file then return nil, err end
    file:write(data)
    file:close()
    return true
end

local function getTable(path)
    return assert(serialization.unserialize(assert(getFile(path))))
end

local function saveTable(path, tbl)
    return assert(saveFile(path, assert(serialization.serialize(tbl))))
end

local tbl = getTable("/etc/system.cfg")
tbl.updateRepo = mainurl

fs.remove(require("superUtiles").getPath())
if serialization.serialize(getTable("/etc/system.cfg")) ~= serialization.serialize(tbl) then
    saveTable("/etc/system.cfg", tbl)
    computer.shutdown("fast")
end
    ]])
end}

if not eeprom then
    table.remove(installers, 2)
end

do
    local branchs = {"main", "dev"}
    local selectBranch = 1

    local obj = {}

    function obj.draw(posY)
        if num + 1 == selectButton then
            gpu.setBackground(0xFFFFFF)
            gpu.setForeground(background)
        else
            gpu.setBackground(background)
            gpu.setForeground(0xFFFFFF)
        end
        local label = "branch: " .. branchs[selectBranch]
        obj.start = setText(label, posY, maxStrLen - 1)
        obj.stop = (obj.start + unicode.len(label)) - 1
    end

    function obj.use()
        selectBranch = selectBranch + 1
        if selectBranch > #branchs then selectBranch = 1 end
        mainurl = superMainurl .. "/" .. branchs[selectBranch]
    end

    table.insert(objs, obj)
end

do
    local obj = {}

    function obj.draw(posY)
        if num + 2 == selectButton then
            gpu.setBackground(0xFFFFFF)
            gpu.setForeground(background)
        else
            gpu.setBackground(background)
            gpu.setForeground(0xFFFFFF)
        end
        local label = "INSTALL"
        obj.start = setText(label, posY)
        obj.stop = (obj.start + unicode.len(label)) - 1
    end

    function obj.use()
        for i, v in ipairs(active) do
            if v then
                installers[i]()
            end
        end
        pcall(computer.setArchitecture, "Lua 5.3")--зашита от моих биосов которые удаляют setArchitecture
        computer.shutdown("fast")
    end

    table.insert(objs, obj)
end

function status(text)
    gpu.setBackground(background)
    gpu.setForeground(0xFFFFFF)
    term.clear()
    setText(text, math.floor(ry / 2))
end

------------------------------------------------------main

gpu.setBackground(background)
gpu.fill(1, 2, rx, ry - 1, " ")

if depth == 1 then
    gpu.setBackground(0xFFFFFF)
    gpu.setForeground(0)
    gpu.fill(1, 1, rx, 1, " ")
    setText(label, 1)
else
    local lLabel = label
    local posX = getPosX(label)
    for i = 1, rx do
        local isText = i >= posX and #lLabel ~= 0
        gpu.setBackground(hue(map(i, 1, rx, 1, 255)))
        gpu.setForeground(0xFFFFFF)
        gpu.set(i, 1, isText and unicode.sub(lLabel, 1, 1) or " ")
        gpu.setBackground(0)
        if isText then
            lLabel = unicode.sub(lLabel, 2, unicode.len(lLabel))
        end
    end
end

while true do
    drawAll()
    
    local eventData = {event.pull()}
    if eventData[1] then
        if eventData[1] == "interrupted" then
            gpu.setBackground(0)
            gpu.setForeground(0xFFFFFF)
            term.clear()
            break
        end
        if eventData[1] == "key_down" then
            if eventData[4] == 200 then
                buttonUp()
            elseif eventData[4] == 208 then
                buttonDown()
            end
        elseif eventData[1] == "scroll" then
            if eventData[5] > 0 then
                buttonUp()
            else
                buttonDown()
            end
        end
        checkUsed(eventData)
    end
end