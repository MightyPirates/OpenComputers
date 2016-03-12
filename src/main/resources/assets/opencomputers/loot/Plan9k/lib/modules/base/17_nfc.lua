programmers = {}

local function buildDevfs()
    for file in pairs(kernel.modules.devfs.data) do
        if file:match("^nfc") then
            kernel.modules.devfs.data[file] = nil
        end
    end
    for k, nfc in ipairs(programmers) do
        kernel.modules.devfs.data["nfc" .. nfc:sub(1,4):upper()] = {
            __type = "f",
            open = function(hnd, mode)
                if mode == "r" then error("Invalid mode") end
                hnd.nfc = nfc
            end,
            size = function()
                return 2048
            end,
            write = function(h, data)
                if component.invoke(nfc, "isDataWaiting") then
                    component.invoke(nfc, "clearNFCData")
                end
                component.invoke(nfc, "writeNFCData", data)
                return true
            end,
        }
    end
end

local function onComponentAdded(_, address, componentType)
    if componentType == "NFCProgrammer" then
        programmers[#programmers + 1] = address
        buildDevfs()
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "NFCProgrammer" then
        local t
        for i, nfc in ipairs(programmers) do
            if nfc == address then
                t = i
                break
            end
        end
        table.remove(programmers, t)
        buildDevfs()
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
