require("process").info().data.signal = function()
    require("term").clear()
    os.exit()
end
local programmData = [[

-- Display the start screen
local w,h = term.getSize()

local titleColour, headingColour, textColour, wormColour, fruitColour
if term.isColour() then
    titleColour = colours.red
    headingColour = colours.yellow
    textColour = colours.white
    wormColour = colours.green
    fruitColour = colours.red
else
    titleColour = colours.white
    headingColour = colours.white
    textColour = colours.white
    wormColour = colours.white
    fruitColour = colours.white
end

local function printCentred( y, s )
    local x = math.floor((w - string.len(s)) / 2)
    term.setCursorPos(x,y)
    --term.clearLine()
    term.write( s )
end

local xVel,yVel = 1,0
local xPos, yPos = math.floor(w/2), math.floor(h/2)
local pxVel, pyVel = nil, nil

local nLength = 1
local nExtraLength = 6
local bRunning = true

local tailX,tailY = xPos,yPos
local nScore = 0
local nDifficulty = 2
local nSpeed, nInterval

-- Setup the screen
local screen = {}
for x=1,w do
    screen[x] = {}
    for y=1,h do
        screen[x][y] = {}
    end
end
screen[xPos][yPos] = { snake = true }

local nFruit = 1
local tFruits = {
    "A", "B", "C", "D", "E", "F", "G", "H",
    "I", "J", "K", "L", "M", "N", "O", "P",
    "Q", "R", "S", "T", "U", "V", "W", "X",
    "Y", "Z",
    "a", "b", "c", "d", "e", "f", "g", "h",
    "i", "j", "k", "l", "m", "n", "o", "p",
    "q", "r", "s", "t", "u", "v", "w", "x",
    "y", "z",
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
    "@", "$", "%", "#", "&", "!", "?", "+", "*", "~"
}

local function addFruit()
    while true do
        local x = math.random(1,w)
        local y = math.random(2,h)
        local fruit = screen[x][y]
        if fruit.snake == nil and fruit.wall == nil and fruit.fruit == nil then
            screen[x][y] = { fruit = true }
            term.setCursorPos(x,y)
            term.setBackgroundColour( fruitColour )
            term.write(" ")
            term.setBackgroundColour( colours.black )
            break
        end
    end
    
    nFruit = nFruit + 1
    if nFruit > #tFruits then
        nFruit = 1
    end
end

local function drawMenu()
    term.setTextColour( headingColour )
    term.setCursorPos(1,1)
    term.write( "SCORE " )
    
    term.setTextColour( textColour )
    term.setCursorPos(7,1)
    term.write( tostring(nScore) )

    term.setTextColour( headingColour )
    term.setCursorPos(w-11,1)
    term.write( "DIFFICULTY ")

    term.setTextColour( textColour )
    term.setCursorPos(w,1)
    term.write( tostring(nDifficulty or "?") ) 

    term.setTextColour( colours.white )
end

local function update( )
    local x,y = xPos,yPos
    if pxVel and pyVel then
        xVel, yVel = pxVel, pyVel
        pxVel, pyVel = nil, nil
    end

    -- Remove the tail
    if nExtraLength == 0 then
        local tail = screen[tailX][tailY]
        screen[tailX][tailY] = {}
        term.setCursorPos(tailX,tailY)
        term.write(" ")
        tailX = tail.nextX
        tailY = tail.nextY
    else
        nExtraLength = nExtraLength - 1
    end
    
    -- Update the head
    local head = screen[xPos][yPos]
    local newXPos = xPos + xVel
    local newYPos = yPos + yVel
    if newXPos < 1 then
        newXPos = w
    elseif newXPos > w then
        newXPos = 1
    end
    if newYPos < 2 then
        newYPos = h
    elseif newYPos > h then
        newYPos = 2
    end
    
    local newHead = screen[newXPos][newYPos]
    if newHead.snake == true or newHead.wall == true then
        bRunning = false
        
    else
        if newHead.fruit == true then
            nScore = nScore + 10
            nExtraLength = nExtraLength + 1
            addFruit()
        end
        xPos = newXPos
        yPos = newYPos
        head.nextX = newXPos
        head.nextY = newYPos
        screen[newXPos][newYPos] = { snake = true }
        
    end
    
    term.setCursorPos(xPos,yPos)
    term.setBackgroundColour( wormColour )
    term.write(" ")
    term.setBackgroundColour( colours.black )

    drawMenu()
end

-- Display the frontend
term.clear()
local function drawFrontend()
    --term.setTextColour( titleColour )
    --printCentred( math.floor(h/2) - 4, " W O R M " )

    term.setTextColour( headingColour )
    printCentred( math.floor(h/2) - 3, "" )
    printCentred( math.floor(h/2) - 2, " SELECT DIFFICULTY " )
    printCentred( math.floor(h/2) - 1, "" )
    
    printCentred( math.floor(h/2) + 0, "            " )
    printCentred( math.floor(h/2) + 1, "            " )
    printCentred( math.floor(h/2) + 2, "            " )
    printCentred( math.floor(h/2) - 1 + nDifficulty, " [        ] " )

    term.setTextColour( textColour )
    printCentred( math.floor(h/2) + 0, "EASY" )
    printCentred( math.floor(h/2) + 1, "MEDIUM" )
    printCentred( math.floor(h/2) + 2, "HARD" )
    printCentred( math.floor(h/2) + 3, "" )

    term.setTextColour( colours.white )
end

drawMenu()
drawFrontend()
while true do
    local e,key = os.pullEvent( "key" )
    if key == keys.up or key == keys.w then
        -- Up
        if nDifficulty > 1 then
            nDifficulty = nDifficulty - 1
            drawMenu()
            drawFrontend()
        end
    elseif key == keys.down or key == keys.s then
        -- Down
        if nDifficulty < 3 then
            nDifficulty = nDifficulty + 1
            drawMenu()
            drawFrontend()
        end
    elseif key == keys.enter then
        -- Enter
        break
    end
end

local tSpeeds = { 5, 10, 25 }
nSpeed = tSpeeds[nDifficulty]
nInterval = 1 / nSpeed

-- Grow the snake to its intended size
term.clear()
drawMenu()
screen[tailX][tailY].snake = true
while nExtraLength > 0 do
    update()
end
addFruit()
addFruit()

-- Play the game
local timer = os.startTimer(0)
while bRunning do
    local event, p1, p2 = os.pullEvent()
    if event == "timer" and p1 == timer then
        timer = os.startTimer(nInterval)
        update( false )
    
    elseif event == "key" then
        local key = p1
        if key == keys.up or key == keys.w then
            -- Up
            if yVel == 0 then
                pxVel,pyVel = 0,-1
            end
        elseif key == keys.down or key == keys.s then
            -- Down
            if yVel == 0 then
                pxVel,pyVel = 0,1
            end
        elseif key == keys.left or key == keys.a then
            -- Left
            if xVel == 0 then
                pxVel,pyVel = -1,0
            end
        
        elseif key == keys.right or key == keys.d then
            -- Right
            if xVel == 0 then
                pxVel,pyVel = 1,0
            end
        
        end    
    end
end

-- Display the gameover screen
term.setTextColour( headingColour )
printCentred( math.floor(h/2) - 2, "                   " )
printCentred( math.floor(h/2) - 1, " G A M E   O V E R " )

term.setTextColour( textColour )
printCentred( math.floor(h/2) + 0, "                 " )
printCentred( math.floor(h/2) + 1, " FINAL SCORE "..nScore.." " )
printCentred( math.floor(h/2) + 2, "                 " )
term.setTextColour( colours.white )

local timer = os.startTimer(2.5)
repeat
    local e,p = os.pullEvent()
    if e == "timer" and p == timer then
        term.setTextColour( textColour )
        printCentred( math.floor(h/2) + 2, " PRESS ANY KEY " )
        printCentred( math.floor(h/2) + 3, "               " )
        term.setTextColour( colours.white )
    end
until e == "char"

term.clear()
term.setCursorPos(1,1)

        
]]
local colorPic = require("colorPic")
local bit32 = require("bit32")
local shell = require("shell")
local su = require("superUtiles")
local fs = require("filesystem")
local unicode = require("unicode")
local component = require("component")
local term = require("term")
local computer = require("computer")
local component = require("component")
local event = require("event")
local keyboard = require("keyboard")
local thread = require("thread")

--------------------------------------------

local gpu = component.gpu
local redstone = component.isAvailable("redstone") and component.redstone

--------------------------------------------

local colors = colorPic.getColors()
local cursorBlick = false

--------------------------------------------

local env
env = {
    fs = {
        list = function(path)
            local files = {}
            for file in fs.list(path) do
                local text = fs.concat(file)
                if unicode.sub(text, unicode.len(text), unicode.len(text)) == "/" then
                    text = unicode.sub(text, unicode.len(text), unicode.len(text) - 1)
                end
                table.insert(files, text)
            end
            return files
        end,
        isDir = fs.isDirectory,
        exists = fs.exists,
        isReadOnly = function(path)
            return fs.get(path).isReadOnly()
        end,
        getName = function(path)
            return fs.get(path).getLabel()
        end,
        makeDir = fs.makeDirectory,
        move = fs.move,
        copy = fs.copy,
        delete = fs.remove,
        combine = fs.concat,
        open = function(path, mode)
            local file, err = io.open(path, mode)
            if not file then return nil, err end

            local obj = {}

            function obj.readAll()
                return su.getFile(path)
            end

            function obj.close()
                return file:close()
            end

            function obj.write(str)
                if type(str) == "string" then
                    return file:write(str)
                else
                    return file:write(string.byte(str))
                end
            end

            function obj.writeLine(str)
                return file:write(str .. "\n")
            end

            function obj.read(bytes)
                if bytes then
                    return file:read(bytes)
                else
                    return string.char(file:read(1))
                end
            end
            
            function obj.readLine()
                return file:read()
            end

            return obj
        end,
    },
    term = {
        getSize = gpu.getResolution,
        isColor = function()
            return gpu.getDepth() ~= 1
        end,
        write = term.write,
        setCursorPos = term.setCursor,
        getCursorPos = term.getCursor,
        clear = term.clear,
        clearLine = term.clearLine,
        scroll = term.scroll,
        getTextColor = gpu.getForeground,
        getBackgroundColor = gpu.getBackground,
        setTextColor = gpu.setForeground,
        setBackgroundColor = gpu.setBackground,
        getCursorBlink = function()
            return cursorBlick
        end,
        setCursorBlink = function(state)
            cursorBlick = state
        end,

        blit = function(char, fore, back)
            local oldFore = gpu.getForeground()
            local oldBack = gpu.getBackground()
            gpu.setForeground(tonumber(fore, 16))
            gpu.setBackground(tonumber(back, 16))
            term.write(char)
            gpu.setForeground(oldFore)
            gpu.setBackground(oldBack)
        end
    },
    os = {
        version = function()
            return "CraftOS 1.8"
        end,
        getComputerID = function()
            return 1
        end,
        getComputerLabel = function()
            return fs.get("/").getLabel()
        end,
        setComputerLabel = function(label)
            fs.get("/").setLabel(label)
        end,
        clock = os.clock,
        time = os.time,
        shutdown = function()
            computer.shutdown()
        end,
        reboot = function()
            computer.shutdown(true)
        end,
        pullEvent = function(name)
            while true do
                local eventData
                if cursorBlick then
                    eventData = {term.pull()}
                else
                    eventData = {event.pull()}
                end
                local newEventData

                if eventData[1] == "touch" and eventData[2] == term.screen() then
                    newEventData = {"mouse_click", math.floor(eventData[5] + 1), math.floor(eventData[3]), math.floor(eventData[4])}
                elseif eventData[1] == "drop" and eventData[2] == term.screen() then
                    newEventData = {"mouse_up", math.floor(eventData[5] + 1), math.floor(eventData[3]), math.floor(eventData[4])}
                elseif eventData[1] == "drag" and eventData[2] == term.screen() then
                    newEventData = {"mouse_drag", math.floor(eventData[5] + 1), math.floor(eventData[3]), math.floor(eventData[4])}
                elseif eventData[1] == "scroll" and eventData[2] == term.screen() then
                    newEventData = {"mouse_scroll", math.floor(-eventData[5]), math.floor(eventData[3]), math.floor(eventData[4])}
                elseif eventData[1] == "key_down" and eventData[2] == term.keyboard() then
                    newEventData = {"key", math.floor(eventData[4]), false}
                    if eventData[3] >= 32 and eventData[3] <= 126 then
                        event.push("ccevent", "char", string.char(eventData[3]))
                    end
                elseif eventData[1] == "key_up" and eventData[2] == term.keyboard() then
                    newEventData = {"key_up", math.floor(eventData[4])}
                elseif eventData[1] == "clipboard" and eventData[2] == term.keyboard() then
                    newEventData = {"paste", eventData[3]}
                elseif eventData[1] == "ccevent" then
                    newEventData = {table.unpack(eventData, 2)}
                end

                if newEventData and (not name or newEventData[1] == name) then
                    return table.unpack(newEventData)
                end
            end
        end,
        startTimer = function(time)
            local id
            id = event.timer(time, function()
                event.push("ccevent", "timer", id)
            end)
            return id
        end,
        queueEvent = function(name, ...)
            event.push("ccevent", name, ...)
        end
    },
    parallel = {
        waitForAny = function(...)
            local threads = {}

            for k, v in pairs({...}) do
                table.insert(threads, thread.create(v))
            end

            pcall(thread.waitForAny, threads)

            for k, v in pairs(threads) do
                v:kill()
            end
        end,
        waitForAll = function(...)
            local threads = {}

            for k, v in pairs({...}) do
                table.insert(threads, thread.create(v))
            end

            pcall(thread.waitForAll, threads)

            for k, v in pairs(threads) do
                v:kill()
            end
        end
    },
    shell = {
        resolve = shell.resolve,
        openTab = function(name, ...)
            shell.execute(name, env, ...)
            return 0
        end,
        switchTab = function(num)
        end
    },
    peripheral = { --заглушка
        isPresent = function()
            return false
        end,
        getType = function()
            return nil
        end,
        getMethods = function()
            return nil
        end,
        call = function()
            return nil
        end,
        wrap = function()
            return nil
        end,
        find = function()
            return nil
        end,
        getNames = function()
            return {}
        end
    },
    settings = {
        set = function()
        end,
        get = function()
        end,
        unset = function()
        end,
        clear = function()
        end,
        getNames = function()
            return {}
        end,
        load = function()
            return false
        end,
        save = function()
            return false
        end
    },

    io = {

    },
    io = io,

    sleep = os.sleep,
    colors = colors,
    keys = keyboard.keys,
    exit = os.exit,
    
    math = math,
    bit = bit32,
    bit32 = bit32,
    type = type,
    string = string,
    table = table,
    tonumber = tonumber,
    tostring = tostring,
    ipairs = ipairs,
    pairs = pairs,
    pcall = pcall,
    xpcall = xpcall,
    error = error,
    debug = debug,
    load = load,
    loadfile = loadfile,
    dofile = dofile,
    assert = assert,
    checkArg = checkArg,
    utf8 = utf8,
    getmetatable = getmetatable,
    setmetatable = setmetatable,
    print = print,
    select = select,
    next = next,
}

env.keys.leftCtrl = env.keys.lcontrol
env.keys.rightCtrl = env.keys.rcontrol
env.keys.backspace = env.keys.back

env.os.pullEventRaw = env.os.pullEvent

env.colours = env.colors
env.term.isColour = env.term.isColor
env.term.setBackgroundColour = env.term.setBackgroundColor
env.term.setTextColour = env.term.setTextColor
env.term.getBackgroundColour = env.term.getBackgroundColor
env.term.getTextColour = env.term.getTextColor

env._G = env

--------------------------------------------

local func = assert(load(programmData, nil, nil, env))
return func(...)
