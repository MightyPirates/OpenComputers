local block = kernel.modules.block

local function writeSectors(drive, data, at)
    local sectorSize = component.invoke(drive, "getSectorSize")
    repeat
        local atSector = math.floor((at - 1) / sectorSize) + 1
        
        local inSectorStart = (at - 1) % sectorSize + 1
        local writable = math.min(#data, sectorSize - inSectorStart + 1)
        
        local old = component.invoke(drive, "readSector", atSector)
        
        local before = old:sub(0, inSectorStart - 1)
        local after = old:sub(inSectorStart + writable)
        
        local toWrite = before .. data:sub(1, writable) .. after
        data = data:sub(writable + 1)
        
        --kernel.io.println("Wd: " .. atSector .. "/" .. #toWrite .. ": "..inSectorStart.." [ " .. writable .. " ] "..(inSectorStart + writable) .. " #old="..#old)
        component.invoke(drive, "writeSector", atSector, toWrite)
        
        at = at + writable
    until #data < 1
end

local function readSectors(drive, at, len)
    local data = ""
    local sectorSize = component.invoke(drive, "getSectorSize")
    repeat
        local atSector = math.floor(at / sectorSize) + 1
        
        sector = component.invoke(drive, "readSector", atSector)
        --kernel.io.println("Rsect " .. atSector .. ": " .. tostring((at - 1) % sectorSize + 1) .. " -> " .. tostring(math.min((at - 1) % sectorSize + len - #data, sectorSize)))
        local read = sector:sub((at - 1) % sectorSize + 1, math.min((at - 1) % sectorSize + len - #data, sectorSize))
        
        data = data .. read
        at = at + #read
    until #data >= len
    return data
end

local function buildDevice(drive)
    return {
        __type = "f",
        open = function(hnd)
            --component.invoke(drive, "seek", -math.huge)
            hnd.drive = drive
            hnd.pos = 1
            --kernel.io.println("Od: " .. hnd.pos .. "/" .. component.invoke(drive, "getCapacity"))
        end,
        size = function()
            return component.invoke(drive, "getCapacity")
        end,
        write = function(h, data)
            
            writeSectors(drive, data, h.pos)
            --kernel.io.println("Wd: " .. h.pos .. "(+" .. #data .. ")/" .. component.invoke(drive, "getCapacity"))
            h.pos = h.pos + #data
            return not (h.pos >= component.invoke(drive, "getCapacity"))
            --TODO: do this correctly
        end,
        read = function(h, len)
            len = math.ceil(len)
            --kernel.io.println("Rd " .. tostring(len) .. ": " .. h.pos .. "/" .. component.invoke(drive, "getCapacity"))
            if h.pos >= component.invoke(drive, "getCapacity") then
                return
            end
            local data = readSectors(drive, h.pos, len)
            h.pos = h.pos + len
            return data
        end,
        seek = function(h, whence, offset)
            offset = offset or 0
            if whence == "end" then
                h.pos = math.min(component.invoke(drive, "getCapacity"), math.max(1, component.invoke(drive, "getCapacity") - offset + 1))
                return h.pos - 1
            elseif whence == "set" then
                h.pos = math.min(component.invoke(drive, "getCapacity"), math.max(1, 1 + offset))
                return h.pos - 1
            else
                h.pos = math.min(component.invoke(drive, "getCapacity"), math.max(1, h.pos + offset))
                return h.pos - 1
            end
            return math.floor(h.pos)
        end
    }
end

local function onComponentAdded(_, address, componentType)
    if componentType == "drive" then
        block.register(address, "sd" .. address:sub(1,4):upper(), buildDevice(address), true)
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "drive" then
         block.unregister(address)
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
