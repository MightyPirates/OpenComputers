local term = {}

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

function term.clear()
    io.write("\x1b[2J")
end

function term.clearLine()
    --io.write("\x1b[K")
end

function term.getCursor()
    io.write("\x1b[6n")
    local code = read("\x1b", "R")
    local y, x = code:match("\x1b%[(%d+);(%d+)R")
    
    return tonumber(x), tonumber(y)
end

function term.getResolution()
    io.write("\x1b[6n\x1b[999;999H\x1b[6n")
    local code = read("\x1b", "R")
    local y, x = code:match("\x1b%[(%d+);(%d+)R")
    code = read("\x1b", "R")
    local h, w = code:match("\x1b%[(%d+);(%d+)R")
    io.write("\x1b[" .. y .. ";" .. x .. "H")
    return tonumber(w), tonumber(h)
end

function term.setCursor(col, row)
  checkArg(1, col, "number")
  checkArg(2, row, "number")
  io.write("\x1b[" .. row .. ";" .. col .. "H")
end

function term.isAvailable()
    return true
end

function term.setCursorBlink(enabled)

end

function term.read(history)
    history = history or {}
    local x, y = 1, 1
    
    local function getLine()
        if not history[y] then
            history[y] = ""
        end
        return history[y]
    end
    
    local function setLine(text)
        y = 1
        history[y] = text
    end
    
    local function insert(char)
        local pre = unicode.sub(getLine(), 1, x - 1)
        local after = unicode.sub(getLine(), x)
        setLine(pre .. char .. after)
        x = x + 1
        io.write("\x1b[K"..char..after.."\x1b["..unicode.len(after).."D")
    end
    
    while true do
        local char = io.read(1)
        if char == "\n" then
            io.write("\n")
            local line = getLine()
            if y == 1 and line ~= "" and line ~= history[2] then
                table.insert(history, 1, "")
            elseif y > 1 and line ~= "" and line ~= history[2] then
                history[1] = line
                table.insert(history, 1, "")
            else
                history[1] = ""
            end
            return line
        elseif char == "\t" then
        elseif char == "\b" and x > 1 then
            local pre = unicode.sub(getLine(), 1, x - 2)
            local after = unicode.sub(getLine(), x)
            setLine(pre .. after)
            x = x - 1
            io.write("\x1bD\x1b[K" .. after .. "\x1b[" .. unicode.len(after) .. "D")
        elseif char == "\x1b" then
            local mode = io.read(1)
            if mode == "[" then
                local act = io.read(1)
                if act == "C" then
                    if unicode.len(getLine()) >= x then
                        io.write("\x1b[C")
                        x = x + 1
                    end
                elseif act == "D" then
                    if x > 1 then
                        io.write("\x1b[D")
                        x = x - 1
                    end
                elseif act == "A" then
                    y = y + 1
                    local line = getLine()
                    --x = math.min(unicode.len(line) + 1, x)
                    io.write("\x1b[" .. (x - 1)  .. "D\x1b[K" .. line)
                    x = unicode.len(line) + 1
                elseif act == "B" then
                    if y > 1 then
                        y = y - 1
                        local line = getLine()
                        --x = math.min(unicode.len(line) + 1, x)
                        io.write("\x1b[" .. (x - 1) .. "D\x1b[K" .. line)
                        x = unicode.len(line) + 1
                    end
                elseif act == "3" and io.read(1) == "~" then
                    local pre = unicode.sub(getLine(), 1, x - 1)
                    local after = unicode.sub(getLine(), x + 1)
                    setLine(pre .. after)
                    --x = x
                    io.write("\x1b[K" .. after .. "\x1b[" .. unicode.len(after) .. "D")
                end
            elseif mode == "0" then
                local act = io.read(1)
                if act == "H" then
                    io.write("\x1b["..(x - 1).."D")
                    x = 1
                elseif act == "F" then
                    local line = getLine()
                    io.write("\x1b[" .. (x - 1)  .. "D\x1b[" .. (unicode.len(line)) .. "C")
                    x = unicode.len(line) + 1
                end
            end
        elseif char:match("[%g%s]") then
            insert(char)
        end
    end
end

function term.write(value, wrap)
    io.write(value)
end

return term
