local shell = require("shell")
local term = require("term")
local fs = require("filesystem")
local unicode = require("unicode")

local args, options = shell.parse(...)

if not args[1] then
    print("Syntax: edit [file]")
    return
end

local file = args[1] or ""

local function read(from, to)
    local started, data
    while true do
        local char = io.read(1)
        if not char then
            error("Broken pipe")
        end
        if not started and char == from then
            started = true
            data = char
        elseif started then
            if char == to then
                return data .. char
            else
                data = data .. char
            end
        end
    end
end

--Cute, isn't it?
io.write("\x1b[999;999H\x1b6n\x1b2J\x1b[30m\x1b[47m\x1bKEdit: " .. file .. "| F1 - save&quit | F3 - just quit\n\x1b[39m\x1b[49m")
local code = read("\x1b", "R")
local h, w = code:match("\x1b%[(%d+);(%d+)R")

local edith = h - 1
local editw = w
local x, y = 1, 1
local atline = 1

local lines = {}

if fs.exists(file) then
    for line in io.lines(file) do
        lines[#lines + 1] = line
    end
end

function setcur()
    io.write("\x1b[" .. (y - atline + 2) .. ";" .. (x) .. "H")
end

local function render(startline, nlines)
    --io.write("\x1b["..(startline - atline + 1)..";1H")
    for n = 1, nlines do
        io.write("\x1b["..(startline - atline + n + 1)..";1H\x1b[K" .. unicode.sub(lines[n + startline - 1] or "", 1, editw))
    end
    setcur()
end

render(1, edith)
setcur()

local run = true
local baseHandler, codeHandler
local charHandler

local code = ""
codeHandler = function(char)
    if char == "[" then code = code .. char
    elseif char == "0" then code = code .. char
    elseif char == "3" then code = code .. char
    elseif code == "[" and char == "A" then
        charHandler = baseHandler
        if y - 1 < 1 then return end
        y = y - 1
        if unicode.len(lines[y]) < x then
            x = unicode.len(lines[y]) + 1
        end
        if y < atline then
            atline = y
            render(y, edith)
        end
        setcur()
    elseif code == "[" and char == "B" then
        charHandler = baseHandler
        y = y + 1
        lines[y] = lines[y] or ""
        if unicode.len(lines[y]) < x then
            x = unicode.len(lines[y]) + 1
        end
        if y > atline + edith - 1 then
            atline = y - edith + 1
            render(y - edith + 1, edith)
        end
        setcur()
    elseif code == "[" and char == "C" then
        charHandler = baseHandler
        if unicode.len(lines[y]) < x then
            y = y + 1
            x = 1
            lines[y] = lines[y] or ""
            if y > atline + edith - 1 then
                atline = y - edith + 1
                render(y - edith + 1, edith)
            end
            setcur()
            return
        end
        x = x + 1
        setcur()
    elseif code == "[" and char == "D" then
        charHandler = baseHandler
        if x - 1 < 1 then
            if y - 1 < 1 then return end
            y = y - 1
            if y < atline then
                atline = y
                render(y, edith)
            end
            x = unicode.len(lines[y]) + 1
            setcur()
            return
        end
        x = x - 1
        setcur()
    elseif code == "[0" and char == "P" or char == "R" then
        run = false
        io.write("\x1b[2J")
        if char == "P" then
            local out = io.open(file, "w")
            local text = ""
            for _, line in ipairs(lines) do
                text = text .. line .. "\n"
            end
            out:write(text)
            out:close()
        end
    elseif code == "[3" and char == "~" then
        charHandler = baseHandler
        if x > unicode.len(lines[y]) then
            lines[y] = lines[y] .. (lines[y + 1] or "")
            table.remove(lines, y + 1)
            render(y, atline + edith - y)
            return
        end
        lines[y] = lines[y]:sub(1, x-1) .. lines[y]:sub(x+1)
        render(y, 1)
    else
        charHandler = baseHandler
    end
end

baseHandler = function(char)
    if char == "\x1b" then
        code = ""
        charHandler = codeHandler
    elseif char == "\n" then
        local line = lines[y]
        lines[y] = unicode.sub(line or "", 1, x - 1)
        table.insert(lines, y + 1, unicode.sub(line or "", x))
        x = 1
        render(y, atline + edith - y - 1)
        y = y + 1
        if y > atline + edith - 1 then
            atline = y - edith + 1
            render(y - edith + 1, edith)
        end
        setcur()
    elseif char == "\b" then
        if x > 1 then
            lines[y] = unicode.sub(lines[y] or "", 1, x-2)..unicode.sub(lines[y] or "", x)
            x = x - 1
            render(y, 1)
        elseif y > 1 then
            x = unicode.len(lines[y - 1]) + 1
            lines[y - 1] = lines[y - 1] .. lines[y]
            table.remove(lines, y)
            y = y - 1
            render(y, atline + edith - y - 1)
        end
    else
        lines[y] = unicode.sub(lines[y] or "", 1, x-1)..char..unicode.sub(lines[y] or "", x)
        render(y, 1)
        x = x + 1
        setcur()
    end
end

charHandler = baseHandler

while run do
    local char = io.read(1)
    charHandler(char)
end

