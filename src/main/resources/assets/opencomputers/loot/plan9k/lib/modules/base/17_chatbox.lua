local block = kernel.modules.block

local function buildDevice()
    return {
        __type = "f",
        open = function(hnd, mode)
            if mode == "r" then error("Invalid mode") end
            hnd.chatbox = chatbox
        end,
        size = function()
            return 2048
        end,
        write = function(h, data)
            component.invoke(chatbox, "say", data)
            return true
        end,
    }
end

local function onComponentAdded(_, address, componentType)
    if componentType == "chat_box" then
        block.register(address, "chatbox" .. address:sub(1,4):upper(), buildDevice(address))
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "chat_box" then
        block.unregister(address)
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)

