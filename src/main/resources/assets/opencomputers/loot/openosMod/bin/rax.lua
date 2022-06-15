local component = require("component")

-- Получаем масштаб в качестве первого аргумента скрипта и корректируем его значение
local scale = tonumber(select(1, ...) or 1)
if not scale or scale > 1 then
  scale = 1
elseif scale < 0.1 then
  scale = 0.1
end

local gpu = component.gpu
local blockCountByWidth, blockCountByHeight = component.proxy(gpu.getScreen()).getAspectRatio()
local maxWidth, maxHeight = gpu.maxResolution()
local proportion = (blockCountByWidth * 2 - 0.5) / (blockCountByHeight - 0.25)
 
local height = scale * math.min(
  maxWidth / proportion,
  maxWidth,
  math.sqrt(maxWidth * maxHeight / proportion)
)

-- Выставляем полученное разрешение
local rx, ry = math.floor(height * proportion), math.floor(height)
gpu.setResolution(rx, ry)

require("superUtiles").saveFile("/etc/resolution.cfg", "rax;" .. tostring(scale))