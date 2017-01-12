local block = kernel.modules.block

local function buildDevice(uuid)
    return {
        __type = "f",
        open = function(hnd, mode)
            if mode == "r" then error("Invalid mode") end
            hnd.nfc = uuid
        end,
        size = function()
            return 2048
        end,
        write = function(h, data)
            if component.invoke(uuid, "isDataWaiting") then
                component.invoke(uuid, "clearNFCData")
            end
            component.invoke(uuid, "writeNFCData", data)
            return true
        end,
    }
end

local function onComponentAdded(_, address, componentType)
    if componentType == "NFCProgrammer" then
        block.register(address, "nfc" .. address:sub(1,4):upper(), buildDevice(address))
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "NFCProgrammer" then
        block.unregister(address)
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
