local gui = require("gui_new").create()
local event = require("event")
local su = require("superUtiles")
local fs = require("filesystem")
local shell = require("shell")
local keyboard = require("keyboard")
local colorPic = require("colorPic")

------------------------------------------

local args, options = shell.parse(...)
local standertX = 32
local standertY = 16
local readonly = false
local rx, ry = gui.gpu.maxResolution()
local path
local selectcolor = 1
if args[1] then path = shell.resolve(args[1]) end
local colors = colorPic.getColorIndex()
--local colors = {0xFFFFFF, 0xFFFF00, 0x770066, 0x00FFFF, 0xFFFF00, 0x00FF00, 0x992288, 0xAAAAAA, 0x999999, 0x00FF66, 0xFF00FF, 0x0000FF, 0x550000, 0x44FF44, 0xFF0000, 0x000000}
local bytes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"}
local drawzoneindex = su.generateRandomID()
local image
local function cleanImage(gpu, posX, posY, sizeX, sizeY)
    gpu.setBackground(0)
    gpu.setForeground(0xFFFFFF)
    gpu.fill(posX, posY, sizeX, sizeY, "░")
end

------------------------------------------

local drawzone

local function createImage(sizeX, sizeY)
    image = {}
    for i = 1, sizeY do
        image[i] = ""
        for i2 = 1, sizeX do
            image[i] = image[i] .. " "
        end
    end
    drawzone.sizeX = #image[1]
    drawzone.sizeY = #image
end

local function loadimage(path)
    local file = assert(io.open(path))
    image = {}
    while true do
        local line = file:readLine()
        if not line then break end
        line = line:sub(1, #line - 1)
        image[#image + 1] = line
    end
    file:close()
    drawzone.sizeX = #image[1]
    drawzone.sizeY = #image
    readonly = fs.get(path).isReadOnly() or options.r
end

local function saveimage(path)
    local file = assert(io.open(path, "w"))
    for i = 1, #image do
        assert(file:write(image[i].."\n"))
    end
    file:close()
end

local function setInImage(image, posX, posY, byte)
    local line = image[posY]
    local str = line:sub(1, posX - 1)
    local str2 = line:sub(posX + 1, #line)
    image[posY] = str .. byte .. str2
end

local function getInImage(image, posX, posY)
    local line = image[posY]
    return line:sub(posX, posX)
end

local function resizeImage(image, newX, newY)
    local sizeY = #image
    local sizeX = #image[1] or 0

    local mainString = ""
    for i = 1, sizeX do
        mainString = mainString .. " "
    end

    if newY > sizeY then
        local newCreate = newY - sizeY
        for i = 1, newCreate do
            image[#image + 1] = mainString
        end
    elseif newY < sizeY then
        local newCreate = sizeY - newY
        for i = 1, newCreate do
            image[#image] = nil
        end
    end
    sizeY = newY

    if newX > sizeX then
        for cy = 1, sizeY do
            local str = image[cy]
            local newCreate = newX - #str
            for i = 1, newCreate do
                str = str .. " "
            end
            image[cy] = str
        end
    elseif newX < sizeX then
        for cy = 1, sizeY do
            local str = image[cy]
            str = str:sub(1, newX)
            image[cy] = str
        end
    end

    drawzone.sizeX = newX
    drawzone.sizeY = newY
end

------------------------------------------

local main = gui.createScene()
drawzone = main.createDrawZone(1, 2, 1, 1, function(gpu, posX, posY, sizeX, sizeY)
    if not image then cleanImage(gpu, posX, posY, sizeX, sizeY) end
    gpu.setBackground(0)
    gpu.setForeground(0xFFFFFF)
    gpu.fill(posX, posY, sizeX, sizeY, "░")
    for linecount = 1, #image do
        local line = image[linecount]
        for pixelcount = 1, #line do
            local pixel = line:sub(pixelcount, pixelcount)
            local number = tonumber(pixel, 16)
            if number then
                gpu.setBackground(colors[number + 1])
                gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), " ")
            end
        end
    end
end, drawzoneindex)
local imagepath = path
local function ioMenager(selected)
    if selected == "new" then
        local oldscene = gui.getScene()
        gui.select(0)

        local sizeX = tonumber(io.read() or "")
        if not sizeX or sizeX <= 0 then 
            gui.splas("input error")
            gui.select(oldscene)
            return 
        end
        local sizeY = tonumber(io.read() or "")
        if not sizeY or sizeY <= 0 then
            gui.splas("input error")
            gui.select(oldscene)
            return 
        end

        gui.select(oldscene)

        createImage(sizeX, sizeY)
        if not options.f then imagepath = nil end
    elseif selected == "open" or selected == "save as" or (selected == "save" and not imagepath) then
        if not options.f then
            local old = gui.getScene()
            gui.select(0)

            local input = io.read()
            if input then
                local path = shell.resolve(input)
                local func = (selected == "open" and loadimage) or saveimage
                local ok, err = pcall(func, path)
                if not ok then
                    gui.splas(err or "unkown")
                else
                    imagepath = path
                    if selected ~= "open" and not options.r then readonly = false end
                end
            end

            gui.select(old)
        else
            gui.splash("fixed mode anable")
        end
    elseif selected == "save" then
        if readonly then gui.splash("read only") return end
        local ok, err = pcall(saveimage, imagepath)
        if not ok then gui.splas(err or "unkown")end
    end
    gui.redraw()
end
local filebutton = main.createButton(1, 1, 6, 1, "file", nil, nil, false, nil, nil, nil, function()
    local selected = gui.context(true, 1, 2, {"new", "open", "save as", "save"}, true)
    if selected then
        ioMenager(selected)
    end
end)
local editbutton = main.createButton(7, 1, 6, 1, "edit", nil, nil, false, nil, nil, nil, function()
    local selected = gui.context(true, 7, 2, {"fill", "full fill", "resize"}, true)
    if selected then
        if selected == "fill" or selected == "full fill" then
            for cy = 1, #image do
                for cx = 1, #image[1] do
                    if selected == "full fill" or getInImage(image, cx, cy) == " " then
                        setInImage(image, cx, cy, bytes[selectcolor])
                    end
                end
            end
        elseif selected == "resize" then
            local oldscene = gui.getScene()
            gui.select(0)

            io.write("sizeX: ")
            local sizeX = tonumber(io.read() or "")
            if not sizeX or sizeX <= 0 then 
                gui.splas("input error")
                gui.select(oldscene)
                return 
            end
            io.write("sizeY: ")
            local sizeY = tonumber(io.read() or "")
            if not sizeY or sizeY <= 0 then 
                gui.splas("input error")
                gui.select(oldscene)
                return 
            end

            gui.select(oldscene)
            resizeImage(image, sizeX, sizeY)
        end
        gui.redraw()
    end
end)
local thisColor = main.createLabel(rx - 7, 17, 8, 3, "", colors[selectcolor])
local buttons = {}
for i = 1, 16 do
    buttons[i] = main.createButton(rx - 7, i, 8, 1, "", colors[i], 0xFFAAFF, false, nil, nil, nil, function()
        selectcolor = i
        thisColor.back = colors[selectcolor]
        thisColor.draw()
    end)
end

if path and fs.exists(path) then
    local ok, err = pcall(loadimage, path)
    if not ok then 
        gui.splas(err or "unkown")
        createImage(standertX, standertY)
    end
else
    createImage(standertX, standertY)
end

gui.select(main)

------------------------------------------

while true do
    local eventData = {event.pull()}
    gui.uploadEvent(table.unpack(eventData))
    if eventData[1] == "key_down" and eventData[2] == gui.keyboard then
        if keyboard.isControlDown() then
            if eventData[4] == keyboard.keys.w then
                gui.exit()
            elseif eventData[4] == keyboard.keys.s then
                ioMenager("save")
            end
        end
    elseif eventData[1] == "drawZone" and (eventData[2] == "touch" or eventData[2] == "drag") and eventData[3] == drawzoneindex then
        local x = eventData[4]
        local y = eventData[5]
        local button = eventData[6]
        local byte = bytes[selectcolor]
        local char = " "
        local gpu = gui.gpu
        if button == 1 then
            byte = " "
            char = "░"
        end
        setInImage(image, x, y, byte)
        local oldb = gpu.setBackground((byte ~= " " and colors[selectcolor]) or 0)
        local oldf = gpu.setForeground(0xFFFFFF)
        gui.gpu.set((drawzone.posX + x) - 1, (drawzone.posY + y) - 1, char)
        gpu.setBackground(oldb)
        gpu.setForeground(oldf)
    end
end