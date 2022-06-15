local term = require("term")
local su = require("superUtiles")
local fs = require("filesystem")
local serialization = require("serialization")
local event = require("event")

------------------------------------

local lib = {}

function lib.getDepth()
    if not term.isAvailable() then return 8 end
    local gpu = term.gpu()
    local depth = math.floor(gpu.getDepth())
    return depth
end

function lib.getColorIndex()
    if lib.colorCache then return lib.colorCache end
    local depth = lib.getDepth()
    local function generate()
        do return {0xFFFFFF, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xFEFE6C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C, 0x999999, 0x4C99B2, 0xB266E5, 0x3333FF, 0x9F664C, 0x57A64E, 0xFF3333, 0x000000} end --да я писал в торопях
        if depth == 4 then
            return {0xFFFFFF, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xFEFE6C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C, 0x999999, 0x4C99B2, 0xB266E5, 0x3333FF, 0x9F664C, 0x57A64E, 0xFF3333, 0x000000}
        else
            return {0xFFFFFF, 0xF2B233, 0xE57FD8, 0x99B2F2, 0xFEFE8C, 0x7FCC19, 0xF2B2CC, 0x4C4C4C, 0x999999, 0x4C99B2, 0xB266E5, 0x3366CC, 0x9F664C, 0x57A64E, 0xCC4C4C, 0x000000}
        end
    end
    local colors
    if depth == 1 then
        return {0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 1, 2, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0x000000}
    elseif depth == 4 then
        if fs.exists("/etc/palette/colorPic/depth4.cfg") then
            colors = assert(serialization.unserialize(assert(su.getFile("/etc/palette/colorPic/depth4.cfg"))))
        else
            colors = generate()
            su.saveFile("/etc/palette/colorPic/depth4.cfg", assert(serialization.serialize(colors)))
        end
    else
        if fs.exists("/etc/palette/colorPic/depth8.cfg") then
            colors = assert(serialization.unserialize(assert(su.getFile("/etc/palette/colorPic/depth8.cfg"))))
        else
            colors = generate()
            su.saveFile("/etc/palette/colorPic/depth8.cfg", assert(serialization.serialize(colors)))
        end
    end
    lib.colorCache = colors
    return colors
end

function lib.removeCache()
    lib.colorCache = nil
end

event.listen("gpu_bound", lib.removeCache)
event.listen("depthChanged", lib.removeCache)

function lib.getColors()
    local colorsIndex = lib.getColorIndex()

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
    return colors
end

function lib.optimize(color)
    local depth = lib.getDepth()
    local colors = lib.getColors()
    color = math.floor(color)

    if depth == 4 then
        local r, g, b = lib.colorUnBlend(color)
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

        local levelR, levelG, levelB = r // 64, g // 64, b // 64
        if (levelR >= 2) and (levelG ~= 0 and levelG ~= 3) and (levelB ~= 0 and levelB ~= 3) then
            return colors.pink
        elseif (levelR == 3) and (levelG >= 1 and levelG <= 2) and (levelB <= 1) then
            return colors.orange
        elseif (levelR == 0) and (levelG >= 2) and (levelB >= 1) then
            return colors.lightBlue
        end
    end
    return color
end

function lib.reverse(color)
    color = 0xFFFFFF - color
    return color
end

function lib.hsvToRgb(h, s, v)
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

    return r, g, b
end

function lib.colorBlend(r, g, b)
    r = math.floor(r)
    g = math.floor(g)
    b = math.floor(b)
    return math.floor(b + (g * 256) + (r * 256 * 256))
end

function lib.colorUnBlend(color)
    color =  math.floor(color)
    local blue = color % 256
    local green = (color // 256) % 256
    local red = (color // (256 * 256)) % 256
    return math.floor(red), math.floor(green), math.floor(blue)
end

return lib
