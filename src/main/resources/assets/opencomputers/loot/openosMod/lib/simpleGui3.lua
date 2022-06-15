local su = require("superUtiles")
local component = require("component")
local unicode = require("unicode")
local term = require("term")
local event = require("event")
local process = require("process")
local computer = require("computer")
local colorPic = require("colorPic")

local function formatString(str, size, mode)
    local str1 = " "

    str = unicode.sub(str, 1, size)
    local value, substr = size - unicode.len(str), str1:rep(size - unicode.len(str))

    if mode == 1 then
        return substr .. str
    elseif mode == 2 then
        str = str1:rep(value // 2) .. str .. str1:rep(value // 2)
        if #str < size then
            str = str .. str1:rep(size - unicode.len(str))
        end
        return str
    else
        return str .. substr
    end
end

return {create = function()
    local lib = {}
    lib.gpu = term.gpu()
    lib.screen = term.screen()
    lib.keyboard = term.keyboard()
    lib.keyboards = term.keyboards()

    lib.depth = math.floor(lib.gpu.getDepth())
    lib.rx, lib.ry = lib.gpu.getResolution()

    lib.colors = colorPic.getColors()
    
    function lib.selectColor(mainColor, simpleColor, bw)
        if type(bw) == "boolean" then bw = bw and 0xFFFFFF or 0 end
        if lib.depth == 4 then
            return simpleColor or mainColor
        elseif lib.depth == 1 then
            return bw
        else
            return mainColor
        end
    end

    lib.blackColor = 0
    lib.whiteColor = 0xFFFFFF
    lib.grayColor = lib.selectColor(lib.colors.lightGray, nil, false)

    function lib.setColor(back, fore)
        lib.gpu.setBackground(back or lib.whiteColor)
        lib.gpu.setForeground(fore or lib.blackColor)
    end

    function lib.clear(back, fore)
        lib.setColor(back, fore)
        lib.gpu.fill(1, 1, lib.rx, lib.ry, " ")
    end

    function lib.invert()
        lib.gpu.setForeground(lib.gpu.setBackground(lib.gpu.getForeground()))
    end

    function lib.setText(text, posY)
        local x = ((lib.rx // 2) - (unicode.len(text) // 2)) + 1
        lib.gpu.set(x, posY, text)
        return x
    end

    lib.isControl = lib.screen and (lib.keyboard or (math.floor(computer.getDeviceInfo()[lib.screen].width) ~= 1))

    function lib.status(text, del)
        if not lib.isControl and del == true then del = 1 end

        local texts = type(text) == "table" and text or su.splitText(text, "\n")
        if del == true then
            table.insert(texts, "press enter or touch to continue")
        end

        lib.clear()
        lib.setColor(lib.whiteColor, lib.grayColor)
        for i, v in ipairs(texts) do
            lib.setText(v, ((lib.ry // 2) - (#texts // 2)) + (i - 1))
        end
        
        if del == true then
            while true do
                local eventData, uuid, _, code, button = event.pull()
                if eventData == "touch" and uuid == lib.screen and button == 0 then
                    break
                elseif eventData == "key_down" and su.inTable(lib.keyboards, uuid) and code == 28 then
                    break
                end
            end
        elseif del then
            os.sleep(del)
        end
    end

    local inputBuf = {}
    function lib.input(text, crypto, emptyAllow)
        local buffer, center, select = "", lib.ry // 2, 0

        local function redraw()
            lib.clear()
            local buffer = buffer
            if crypto then
                local str1 = "*"
                buffer = str1:rep(unicode.len(buffer))
            end

            local drawtext = text .. ": " .. buffer .. "_"
            lib.setColor(lib.whiteColor, lib.grayColor)
            lib.setText(drawtext, center)
        end

        while 1 do
            redraw()
            local eventName, uuid, char, code = event.pull()
            if eventName then
                if eventName == "key_down" and su.inTable(lib.keyboards, uuid) then
                    if code == 28 then
                        if #buffer > 0 or emptyAllow then
                            if not crypto and buffer ~= inputBuf[1] and buffer ~= "" then
                                table.insert(inputBuf, 1, buffer)
                            end
                            return buffer
                        end
                    elseif code == 200 or code == 208 then
                        buffer = ""
                        if code == 200 then
                            if select < #inputBuf then
                                select = select + 1
                            end
                        else
                            if select > 0 then
                                select = select - 1
                            end
                        end
                        buffer = inputBuf[select] or ""
                        redraw()
                    elseif code == 14 then
                        if unicode.len(buffer) > 0 then
                            select = 0
                            buffer = unicode.sub(buffer, 1, unicode.len(buffer) - 1)
                            redraw()
                        end
                    elseif char == 3 then
                        return nil
                    elseif char >= 32 and char <= 127 then
                        select = 0
                        buffer = buffer .. unicode.char(char)
                        redraw()
                    end
                elseif eventName == "clipboard" and su.inTable(lib.keyboards, uuid) then
                    select = 0
                    buffer = buffer .. char
                    if unicode.sub(char, unicode.len(char), unicode.len(char)) == "\n" then
                        local data = unicode.sub(buffer, 1, unicode.len(buffer) - 1)
                        if not crypto and inputBuf[1] ~= data and inputBuf[1] ~= "" then
                            table.insert(inputBuf, 1, data)
                        end
                        return data
                    end
                elseif eventName == "touch" and uuid == lib.screen then
                    if #buffer == 0 then
                        return nil
                    end
                end
            end
        end
    end

    function lib.menu(label, inStrs, num)
        if not num or num < 1 then num = 1 end

        local max = 0
        for i, v in ipairs(inStrs) do
            if unicode.len(v) > max then
                max = unicode.len(v)
            end
        end

        local strs = {}
        table.insert(strs, "")
        for i, v in ipairs(inStrs) do
            table.insert(strs, formatString(v, max + 4, 2))
        end

        local pos, posY, oldpos, poss = (num or 1) + 1, (lib.ry // 2) - (#strs // 2), nil, {}
        if posY < 1 then posY = 1 end
        while 1 do
            local startpos = (pos // lib.ry) * lib.ry

            if pos ~= oldpos then
                lib.clear()
                if startpos == 0 then
                    lib.setColor(lib.selectColor(lib.whiteColor, nil, false), lib.selectColor(lib.blackColor, nil, true))
                    lib.setText(label, posY)
                end
                lib.setColor(lib.whiteColor, lib.selectColor(lib.grayColor, nil, false))
                for i = 1, #strs do
                    local drawpos = (posY + i) - startpos
                    if drawpos >= 1 then
                        if drawpos > lib.ry then break end
                        if i == pos then lib.invert() end
                        poss[i] = lib.setText(strs[i], drawpos)
                        if i == pos then lib.invert() end
                    end
                end
            end

            local eventData = {event.pull()}
            oldpos = pos
            if #eventData > 0 then
                if eventData[1] == "key_down" and su.inTable(lib.keyboards, eventData[2]) then
                    if eventData[4] == 28 then
                        break
                    elseif eventData[4] == 200 then
                        pos = pos - 1
                    elseif eventData[4] == 208 then
                        pos = pos + 1
                    end
                elseif eventData[1] == "scroll" and eventData[2] == lib.screen then
                    pos = pos - eventData[5]
                elseif eventData[1] == "touch" and eventData[2] == lib.screen and eventData[5] == 0 then
                    local ty = (eventData[4] - posY) + startpos
                    if ty >= 2 and
                    ty <= #strs and
                    eventData[3] >= poss[ty] and
                    eventData[3] < (poss[ty] + unicode.len(strs[ty])) then
                        pos = ty
                        break
                    end
                end
                if pos < 2 then pos = 2 end
                if pos > #strs then pos = #strs end
            end
        end
        return pos - 1, inStrs[pos - 1]
    end

    function lib.yesno(label, simple, num)
        if simple then
            return lib.menu(label, {"no", "yes"}, num) == 2
        else
            return lib.menu(label, {"no", "no", "yes", "no"}, num) == 3
        end
    end

    function lib.exit()
        lib.gpu.setBackground(0)
        lib.gpu.setForeground(0xFFFFFF)
        term.clear()
        os.exit()
    end
    process.info().data.signal = lib.exit

    return lib
end}