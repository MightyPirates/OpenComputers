local term = {}

term.escape = "\x1b"
local write = function(text) io.write(term.escape..text) end
local read = function(from, to)
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

term.color={}
term.color.black=30
term.color.red=31
term.color.green=32
term.color.yellow=33
term.color.blue=34
term.color.magenta=35
term.color.cyan=36
term.color.white=37

term.attr={}
term.attr.resetAllAttr=0
term.attr.bright=1
term.attr.dim=2
term.attr.blink=5
term.attr.reverse=7
term.attr.hidden=8

------------------------------------------
--Set Display Attributes
------------------------------------------

--Set Attribute Mode	*ESC*[{attr1};...;{attrn}m
function term.setAttr(attr)
   write(attr.."m") 
end

function term.setForeground(color)
    write(color.."m")
end

function term.setBackground(color)
    write((color+10).."m")
end

------------------------------------------
--Erasing Text
------------------------------------------

--Erase End of Line	*ESC*[K
--Erases from the current cursor position to the end of the current line.
function term.eraseEndOfLine()
    write("[K")
end

--Erase Start of Line *ESC*[1K
--Erases from the current cursor position to the start of the current line.
function term.eraseStartOfLine()
    write("[1K")
end

--Erase Line *ESC*[2K
--Erases the entire current line.
function term.eraseLine()
    write("[2K")
end
term.clearLine = term.eraseLine

--Erase Down *ESC*[J
--Erases the screen from the current line down to the bottom of the screen.
function term.eraseDown()
    write("[J")
end

--Erase Up *ESC*[1J
--Erases the screen from the current line up to the top of the screen.
function term.eraseUp()
    write("[1J")
end

--Erase Screen *ESC*[2J
--Erases the screen with the background colour and moves the cursor to home.
function term.clear()
    write("[2J")
end


-------------------------------------------
--Tab Control
------------------------------------------

--Set Tab *ESC*H
--Sets a tab at the current position.
function term.tab()
    write("[H")
end

--Clear Tab *ESC*[g
--Clears tab at the current position.
function term.clearTab()
    write("[g")
end

--Clear All Tabs *ESC*[3g
--Clears all tabs.
function term.clearTabs()
    write("[3g")
end


------------------------------------------
--Scrolling
------------------------------------------

--Scroll Screen	*ESC*[r
--Enable scrolling for entire display.
function term.enableScroll()
    write("[r")
end

--Scroll Screen	*ESC*[{start};{end}r
--Enable scrolling from row {start} to row {end}.
function term.scrollScreen(from,to)
    write(string.format("[%d;%dr",from,to))
end

--Scroll Down *ESC*D
--Scroll display down one line.
function term.scrollScreenDown()
    write("D")
end

--Scroll Up *ESC*M
--Scroll display up one line.
function term.scrollScreenUp()
    write("M")
end


------------------------------------------
--Cursor Control
------------------------------------------

--Cursor Home *ESC*[{ROW};{COLUMN}H
--Sets the cursor position where subsequent text will begin. If no row/column parameters are provided (ie. *ESC*[H), the cursor will move to the home position, at the upper left of the screen.
function term.setCursorPosition(row,col)
    write(string.format("[%d;%dH", row, col))
end

function term.resetCursor()
    write("[H")
end

--Cursor Up	*ESC*[{COUNT}A
--Moves the cursor up by COUNT rows; the default count is 1.
function term.cursorUp(count)
    write(string.format("[%dA",(count or 1)))
end

--Cursor Down *ESC*[{COUNT}B
--Moves the cursor down by COUNT rows; the default count is 1.
function term.cursorDown(count)
    write(string.format("[%dB",(count or 1)))
end

--Cursor Forward *ESC*[{COUNT}C
--Moves the cursor forward by COUNT columns; the default count is 1.
function term.cursorForward(count)
    write(string.format("[%dC",(count or 1)))
end

--Cursor Backward *ESC*[{COUNT}D
--Moves the cursor backward by COUNT columns; the default count is 1.
function term.cursorBackward(count)
    write(string.format("[%dD",(count or 1)))
end

--Force Cursor Position	*ESC*[{ROW};{COLUMN}f
--Identical to Cursor Home.
function term.forceCursorPosition(row, col)
    write(string.format("[%d;%df", row, col))
end

--Save Cursor *ESC*[s
--Save current cursor position.
function term.saveCursor()
    write("[s")
end

--Unsave Cursor	*ESC*[u
--Restores cursor position after a Save Cursor.
function term.restoreCursor()
    write("[u")
end

--Save Cursor & Attrs *ESC*7
--Save current cursor position.
function term.saveCursorAndAttr()
    write("7")
end

--Restore Cursor & Attrs *ESC*8
--Restores cursor position after a Save Cursor.
function term.restoreCursorAndAttr()
    write("8")
end


------------------------------------------
--Terminal Setup
------------------------------------------

--Reset Device *ESC*c
--Reset all terminal settings to default.
function term.reset()
    write("c")
end

--Enable Line Wrap *ESC*[7h
--Text wraps to next line if longer than the length of the display area.
function term.enableLineWrap()
    write("[7h")
end

--Disable Line Wrap	*ESC*[7l
--Disables line wrapping.
function term.disableLineWrap()
    write("[7l")
end

------------------------------------------
--Plan9k codes
------------------------------------------

-- \x1b9[H];[W]R - set resolution
function term.setResolution(height,width)
    write(string.format("9%d;%dR", height, width))
end
    
-- \x1b9[Row];[Col];[Height];[Width]F -- fill
function term.fill(row, col, height, width)
   write(string.format("9%d;%d;%d;%dF", row, col, height, width)) 
end

-- \x1b9[Row];[Col];[Height];[Width];[Dest Row];[Dest Col]c -- copy
function term.copy(row, col, height, width, destRow, destCol)
   write(string.format("9%d;%d;%d;%d;%d;%dc", row, col, height, width, destRow, destCol )) 
end

--get resolution
function term.getResolution()
    local y, x = term.getCursorPosition()
    term.setCursorPosition(999,999)
    local h, w = term.getCursorPosition()
    term.setCursorPosition(y,x)
    return tonumber(h), tonumber(w)
end

function term.getInfo()
    io.write("\x1b9I")
    local code = read("\x1b", "I")
    local gpu, screen = code:match("\x1b9([^;]+);([^;]+)I")
    
    return gpu, screen
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

function term.read(history, dobreak, hint, pwchar)
    history = history or {}
    local x, y = 1, 1
    
    local completions = {}
    local lastCompletion = nil
    local refreshHint = false
    local basePart = ""
    
    local function resetCompletion()
        completions = {}
        lastCompletion = nil
        refreshHint = true
    end
    
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
    
    local function insert(str)
        local pre = unicode.sub(getLine(), 1, x - 1)
        local after = unicode.sub(getLine(), x)
        str = pwchar and pwchar:rep(unicode.len(str)) or str
        setLine(pre .. str .. after)
        x = x + unicode.len(str)
        io.write("\x1b[K"..str..after.."\x1b["..unicode.len(after).."D")
        resetCompletion()
    end
    
    while true do
        local char = io.read(1)
        --if char then print(#char) else print("WUT")end
        if not char then
            --WTF?
        elseif char == "\n" then
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
            resetCompletion()
            return line
        elseif char == "\t" and hint then
            if refreshHint then
                resetCompletion()
                refreshHint = false
                basePart = getLine()
                if type(hint) == "function" then
                    completions = hint(getLine(), x) or {}
                else
                    completions = hint or {}
                end
            end
            local cur, completion = next(completions, lastCompletion)
            lastCompletion = cur
            if not completion then
                completion = basePart
            end
            setLine(completion)
            io.write("\x1b[" .. (x - 1) .. "D\x1b[K" .. completion)
            x = unicode.len(completion) + 1
        elseif char == "\b" and x > 1 then
            local pre = unicode.sub(getLine(), 1, x - 2)
            local after = unicode.sub(getLine(), x)
            setLine(pre .. after)
            x = x - 1
            io.write("\x1b[D\x1b[K" .. after .. "\x1b[" .. unicode.len(after) .. "D")
            resetCompletion()
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
            resetCompletion()
        elseif char:match("[%g%s]") then
            insert(char)
        end
    end
end

function term.write(value, wrap)
    io.write(value)
end

return term
