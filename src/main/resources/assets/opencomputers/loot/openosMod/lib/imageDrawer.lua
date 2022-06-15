local term = require("term")
local fs = require("filesystem")
local colorPic = require("colorPic")

-----------------------------------------

local lib = {}

lib.gpu = term.gpu()

function lib.loadimage(path)
    checkArg(1, path, "string")

    -----------------------------------------

    local obj = {}
    obj.image = {}
    obj.colors = colorPic.getColorIndex()

    if path then
        local file = assert(io.open(path))
        while true do
            local line = file:read()
            if not line then break end
            table.insert(obj.image, line)
        end
        file:close()
    end
    
    function obj.draw(posX, posY)
        checkArg(1, posX, "number")
        checkArg(2, posY, "number")

        -----------------------------------------

        local gpu = lib.gpu
        local image = obj.image
        local colors = obj.colors
        local depth = math.floor(gpu.getDepth())

        local oldb = gpu.getBackground()
        for linecount = 1, #image do
            local line = image[linecount]
            for pixelcount = 1, #line do
                local pixel = line:sub(pixelcount, pixelcount)
                local number = tonumber(pixel, 16)
                
                if number then
                    if depth == 1 then
                        local color = colors[number + 1]
                        if color == 0xFFFFFF then
                            gpu.setBackground(0xFFFFFF)
                            gpu.setForeground(0)
                            gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), " ")
                        else
                            gpu.setBackground(0)
                            gpu.setForeground(0xFFFFFF)
                        end
                        if color == 2 then
                            gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), "▓")
                        elseif color == 1 then
                            gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), "▒")
                        elseif color == 0 then
                            gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), " ")
                        end
                    else
                        gpu.setBackground(colors[number + 1])
                        gpu.set(posX + (pixelcount - 1), posY + (linecount - 1), " ")
                    end
                end
            end
        end
        gpu.setBackground(oldb)
    end

    function obj.getSize()
        local image = obj.image
        return #image[1] or 0, #image
    end

    return obj
end

return lib