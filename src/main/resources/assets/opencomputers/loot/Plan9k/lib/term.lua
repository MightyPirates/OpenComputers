local term = {}

function term.clear()
    io.write("\x1b[2J")
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
                end
            end
        elseif char:match("[%g%s]") then
            insert(char)
        end
    end
end

return term
