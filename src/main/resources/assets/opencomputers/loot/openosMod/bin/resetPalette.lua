local gpu = require("term").gpu()
local su = require("superUtiles")
local colorPic = require("colorPic")
local serialization = require("serialization")
local fs = require("filesystem")

local depth = math.floor(gpu.getDepth())
if depth == 1 then return end

local colors = {}

if depth == 4 then
    if fs.exists("/etc/palette/gpu/depth4.cfg") then
        colors = assert(serialization.unserialize(assert(su.getFile("/etc/palette/gpu/depth4.cfg"))))
    else
        for i, v in ipairs(colorPic.getColorIndex()) do--цвета на 4 bit будут мягче и почьти все оналогичьны computer craft
            table.insert(colors, v)
        end
        su.saveFile("/etc/palette/gpu/depth4.cfg", assert(serialization.serialize(colors)))
    end
elseif depth == 8 then
    if fs.exists("/etc/palette/gpu/depth8.cfg") then
        colors = assert(serialization.unserialize(assert(su.getFile("/etc/palette/gpu/depth8.cfg"))))
    else
        for i = 0, 15 do --сброс палитры, она должна быть определена в автозагрузке/конфиге и точька
            local count = su.mapClip(i, 0, 15, 0, 255)
            table.insert(colors, colorPic.colorBlend(count, count, count))
        end
        su.saveFile("/etc/palette/gpu/depth8.cfg", assert(serialization.serialize(colors)))
    end
end

for i, v in ipairs(colors) do
    gpu.setPaletteColor(i - 1, v)
end