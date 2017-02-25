local pipes = require("pipes")
local blinkState = false
local args = {...}

--local screen = component.list('screen')()
--for address in component.list('screen') do
--    if #component.invoke(address, 'getKeyboards') > 0 then
--        screen = address
--    end
--end

local gpu = args[1] --component.list("gpu", true)()
local screen = args[2]
local blink = true
local w, h
if gpu then
    --component.invoke(gpu, "bind", screen)
    w, h = component.invoke(gpu, "getResolution")
    component.invoke(gpu, "setResolution", w, h)
    component.invoke(gpu, "setBackground", 0x000000)
    component.invoke(gpu, "setForeground", 0xFFFFFF)
    component.invoke(gpu, "fill", 1, 1, w, h, " ")
end
local y = 1
local x = 1

local function checkCoord()
    if x < 1 then x = 1 end
    if x > w then x = w end
    if y < 1 then y = 1 end
    if y > h then y = h end
end

local preblinkbg = 0x000000
local preblinkfg = 0x000000

local function unblink()
    if blinkState then
        blinkState = not blinkState
        local char, fg, bg = component.invoke(gpu, "get", x, y)
        preblinkbg = blinkState and bg or preblinkbg
        preblinkfg = blinkState and fg or preblinkfg
        local oribg, obpal = component.invoke(gpu, "setBackground", blinkState and 0xFFFFFF or preblinkbg)
        local orifg, ofpal = component.invoke(gpu, "setForeground", blinkState and 0x000000 or preblinkfg)
        component.invoke(gpu, "set", x, y, char  or " ")
        component.invoke(gpu, "setBackground", oribg)
        component.invoke(gpu, "setForeground", orifg)
    end
end

local function reblink()
    if not blinkState and blink then
        blinkState = not blinkState
        local char, fg, bg = component.invoke(gpu, "get", x, y)
        preblinkbg = blinkState and bg or preblinkbg
        preblinkfg = blinkState and fg or preblinkfg
        local oribg, obpal = component.invoke(gpu, "setBackground", blinkState and 0xFFFFFF or preblinkbg)
        local orifg, ofpal = component.invoke(gpu, "setForeground", blinkState and 0x000000 or preblinkfg)
        component.invoke(gpu, "set", x, y, char  or " ")
        component.invoke(gpu, "setBackground", oribg)
        component.invoke(gpu, "setForeground", orifg)
    end
end

local scrTop = 1
local scrBot = nil

local function scroll()
    unblink()
    scrBot = scrBot or h
    x = 1
    if y == h then
        component.invoke(gpu, "copy", 1, scrTop + 1, w, scrBot - scrTop, 0, -1)
        component.invoke(gpu, "fill", 1, scrBot, w, 1, " ")
    else
        y = y + 1
    end
    reblink()
end

local printBuf = ""

local function printBuffer()
    if #printBuf < 1 then return end
    component.invoke(gpu, "set", x, y, printBuf)
    if x == w then
        scroll()
    else
        x = x + unicode.len(printBuf)
        checkCoord()
    end
    printBuf = ""
    if pipes.shouldYield() then
        os.sleep()
    end
end

local function backDelChar()
    if #printBuf > 0 then
        printBuf = unicode.sub(printBuf, 1, unicode.len(printBuf) - 1)
    else
        x = x - 1
        unblink()
        component.invoke(gpu, "set", x, y, " ")
        reblink()
    end
end

---Char handlers

local charHandlers = {}

function charHandlers.base(char)
    if char == "\n" then
        printBuffer()
        scroll()
    elseif char == "\r" then
        unblink()
        printBuffer()
        x = 1
        reblink()
    elseif char == "\t" then
        printBuf = printBuf .. "  "
    elseif char == "\b" then
        backDelChar()
    elseif char == "\x1b" then
        charHandlers.active = charHandlers.control
        charHandlers.control(char)
    elseif char:match("[%g%s]") then
        printBuf = printBuf .. char
    end
end

local mcommands = {}
local swap = false

mcommands["7"] = function()
    local fc, fp = component.invoke(gpu, "getForeground")
    local bc, bp = component.invoke(gpu, "getBackground")
    
    component.invoke(gpu, "setForeground", bc, bp)
    component.invoke(gpu, "setBackground", fc, fp)
    swap = true
end

mcommands["0"] = function()
    if swap then
        local fc, fp = component.invoke(gpu, "getForeground")
        local bc, bp = component.invoke(gpu, "getBackground")
        
        component.invoke(gpu, "setForeground", bc, bp)
        component.invoke(gpu, "setBackground", fc, fp)
    end
    swap = false
end

mcommands["1"] = function()end --Bold font
mcommands["2"] = function()end --Dim font
mcommands["3"] = function()end --Italic
mcommands["4"] = function()end --Underscore
mcommands["10"] = function()end --Select primary font (LA100)

mcommands["30"] = function()component.invoke(gpu, "setForeground", 0x000000)end
mcommands["31"] = function()component.invoke(gpu, "setForeground", 0xFF0000)end
mcommands["32"] = function()component.invoke(gpu, "setForeground", 0x00FF00)end
mcommands["33"] = function()component.invoke(gpu, "setForeground", 0xFFFF00)end
mcommands["34"] = function()component.invoke(gpu, "setForeground", 0x0000FF)end
mcommands["35"] = function()component.invoke(gpu, "setForeground", 0xFF00FF)end
mcommands["36"] = function()component.invoke(gpu, "setForeground", 0x00FFFF)end
mcommands["37"] = function()component.invoke(gpu, "setForeground", 0xFFFFFF)end

mcommands["40"] = function()component.invoke(gpu, "setBackground", 0x000000)end
mcommands["41"] = function()component.invoke(gpu, "setBackground", 0xFF0000)end
mcommands["42"] = function()component.invoke(gpu, "setBackground", 0x00FF00)end
mcommands["43"] = function()component.invoke(gpu, "setBackground", 0xFFFF00)end
mcommands["44"] = function()component.invoke(gpu, "setBackground", 0x0000FF)end
mcommands["45"] = function()component.invoke(gpu, "setBackground", 0xFF00FF)end
mcommands["46"] = function()component.invoke(gpu, "setBackground", 0x00FFFF)end
mcommands["47"] = function()component.invoke(gpu, "setBackground", 0xFFFFFF)end

mcommands["39"] = function()component.invoke(gpu, "setForeground", 0xFFFFFF)end
mcommands["49"] = function()component.invoke(gpu, "setBackground", 0x000000)end

local lcommands = {}

lcommands["4"] = function()end --Reset to replacement mode
lcommands["?25"] = function()
    blink = false
end

local hcommands = {}

hcommands["?25"] = function()
    blink = true
end

local ncommands = {}

ncommands["6"] = function()io.write("\x1b[" .. math.floor(y) .. ";" .. math.floor(x) .. "R")end

local commandMode = ""
local commandBuf = ""
local commandList = {}

--TODO \x1b[C -- reset term to initial state
--TODO: REFACTOR INTO FUNCTION ARRAY

--TODO: p9-codes:
-- \x1b9[H];[W]R - set resolution
-- \x1b9[Row];[Col];[Height];[Width]F -- fill
-- \x1b9[Row];[Col];[Height];[Width];[Dest Row];[Dest Col]c -- copy

--Add fake gpu component for compat(?)

local control = {}

control["\x1b"] = function(char) --Begin
    commandList = {}
    commandBuf = ""
    commandMode = ""
    unblink()
    return true
end

control["["] = function(char)
    if commandMode ~= "" or commandBuf ~= "" then
        charHandlers.active = charHandlers.base
        reblink()
        return true
    end
    commandMode = "["
    return true
end

control["("] = function(char)
    if commandMode ~= "" or commandBuf ~= "" then
        charHandlers.active = charHandlers.base
        reblink()
        return true
    end
    commandMode = "("
    return true
end

control["9"] = function(char)
    if commandMode == "" and commandBuf == "" then
        commandMode = "9"
    else
        commandBuf = commandBuf .. char
        return true
    end
    return true
end

control[";"] = function(char)
    commandList[#commandList + 1] = commandBuf
    commandBuf = ""
    return true
end

control["m"] = function(char)
    commandList[#commandList + 1] = commandBuf
    if not commandList[1] or commandList[1] == "" then
        commandList[1] = "0"
    end
    for _, command in ipairs(commandList) do
        if not mcommands[command] then
            pipes.log("Unknown escape code: " .. tostring(command))
            break
        end
        mcommands[command]()
    end
end

control["l"] = function(char)
    commandList[#commandList + 1] = commandBuf
    if not commandList[1] or commandList[1] == "" then
        commandList[1] = "0"
    end
    for _, command in ipairs(commandList) do
        if not lcommands[command] then
            pipes.log("Unknown escape code: " .. tostring(command))
            break
        end
        lcommands[command]()
    end
end

control["h"] = function(char)
    commandList[#commandList + 1] = commandBuf
    if not commandList[1] or commandList[1] == "" then
        commandList[1] = "0"
    end
    for _, command in ipairs(commandList) do
        if not hcommands[command] then
            pipes.log("Unknown escape code: " .. tostring(command))
            break
        end
        hcommands[command]()
    end
end

control["n"] = function(char)
    commandList[#commandList + 1] = commandBuf
    if not commandList[1] or commandList[1] == "" then
        commandList[1] = "0"
    end
    for _, command in ipairs(commandList) do
        if not ncommands[command] then
            pipes.log("Unknown escape code: " .. tostring(command))
            break
        end
        ncommands[command]()
    end
end

control["d"] = function(char)
    commandList[#commandList + 1] = commandBuf
    local n = tonumber(commandList[1]) or 1
    y = math.max(n, 1)
    checkCoord()
end

control["R"] = function(char) --Set resolution
    if commandMode == "9" then
        commandList[#commandList + 1] = commandBuf
        local nh, nw = tonumber(commandList[1]) or h, tonumber(commandList[2]) or w
        if x > nw then x = math.max(nw, 1) end
        if y > nh then y = math.max(nw, 1) end
        if component.invoke(gpu, "setResolution", nw, nh) then
            w = nw
            h = nh
        end
    end
end

control["I"] = function(char) --Term info
    if commandMode == "9" then
        io.write("\x1b9" .. gpu .. ";" .. screen .. "I")
    end
end

control["!"] = function(char) --Disable
    if commandMode == "9" then
        charHandlers.active = function(c)
            if c == "\255" or c == "!" then
                commandList = {}
                commandBuf = ""
                commandMode = ""
                charHandlers.active = charHandlers.base
                blink = true
            end
        end
        blink = false
        return true
    end
end

-- \x1b9[Row];[Col];[Height];[Width];[Dest Row];[Dest Col]c -- copy
control["c"] = function(char)
    if commandMode == "9" then
        commandList[#commandList + 1] = commandBuf
        if #commandList == 6 then
            component.invoke(gpu,
                             "copy",
                             tonumber(commandList[2]),
                             tonumber(commandList[1]),
                             tonumber(commandList[4]),
                             tonumber(commandList[3]),
                             tonumber(commandList[6]),
                             tonumber(commandList[5]))
        end
    end
end

control["r"] = function(char) --Set scroll region
    commandList[#commandList + 1] = commandBuf
    local nt, nb = tonumber(commandList[1]) or 1, tonumber(commandList[2]) or h
    scrTop = nt
    scrBot = nb
end

control["H"] = function(char) --set pos
    commandList[#commandList + 1] = commandBuf
    local ny, nx = tonumber(commandList[1]), tonumber(commandList[2])
    x = math.min(nx or 1, w)
    y = math.min(ny or 1, h)
    checkCoord()
end
control["f"] = control["H"]


control["A"] = function(char) --Move up
    commandList[#commandList + 1] = commandBuf
    local n = tonumber(commandList[1]) or 1
    y = y - n
    checkCoord()
end

control["B"] = function(char) --Move down
    if commandMode == "(" then
        charHandlers.active = charHandlers.base
        reblink()
        return true
    end
    commandList[#commandList + 1] = commandBuf
    local n = tonumber(commandList[1]) or 1
    y = math.max(y - n, 1)
    checkCoord()
end

control["C"] = function(char) --Move forward
    commandList[#commandList + 1] = commandBuf
    local n = tonumber(commandList[1]) or 1
    x = x + n
    checkCoord()
end

control["D"] = function(char)
    commandList[#commandList + 1] = commandBuf
    local n = tonumber(commandList[1]) or 1
    x = math.max(x - n, 1)
    checkCoord()
end

control["G"] = function(char) --Cursor Horizontal position Absolute
    commandList[#commandList + 1] = commandBuf
    x = tonumber(commandList[1]) or 1
    checkCoord()
end

control["J"] = function(char) --Clear
    commandList[#commandList + 1] = commandBuf
    if commandList[1] == "2" then
        component.invoke(gpu, "fill", 1, 1, w, h, " ")
        x, y = 1, 1
    end
end

control["K"] = function(char) --Erase to end of line
    commandList[#commandList + 1] = commandBuf
    component.invoke(gpu, "fill", x, y, w - x, 1, " ")
end

control["X"] = function(char) --Erase next chars
    commandList[#commandList + 1] = commandBuf
    component.invoke(gpu, "fill", x, y, tonumber(commandList[1]) or 1, 1, " ")
end

function charHandlers.control(char)
    if control[char] and not control[char](char) then
        charHandlers.active = charHandlers.base
        reblink()
        commandList = {}
        commandBuf = ""
        commandMode = ""
    elseif not control[char] then
        commandBuf = commandBuf .. char
    end
end

---Char handler end

charHandlers.active = charHandlers.base

local function _print(msg)
    if gpu then
        
        for char in msg:gmatch(".") do
            charHandlers.active(char)
        end
        
        printBuffer()
    end
end

pipes.setTimer(function()
    if blink then
        blinkState = not blinkState
        local char, fg, bg = component.invoke(gpu, "get", x, y)
        preblinkbg = blinkState and bg or preblinkbg
        preblinkfg = blinkState and fg or preblinkfg
        local oribg, obpal = component.invoke(gpu, "setBackground", blinkState and 0xFFFFFF or preblinkbg)
        local orifg, ofpal = component.invoke(gpu, "setForeground", blinkState and 0x000000 or preblinkfg)
        component.invoke(gpu, "set", x, y, char  or " ")
        component.invoke(gpu, "setBackground", oribg)
        component.invoke(gpu, "setForeground", orifg)
    end
end, 0.5)

while true do
    local data = io.read(1)
    if io.input().remaining() > 0 then
        data = data .. io.read(io.input().remaining())
    end
    unblink()
    _print(data)
end
