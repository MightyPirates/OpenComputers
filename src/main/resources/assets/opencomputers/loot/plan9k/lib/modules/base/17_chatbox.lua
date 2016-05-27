chatboxes = {}

local function buildDevfs()
    for file in pairs(kernel.modules.devfs.data) do
        if file:match("^chatbox") then
            kernel.modules.devfs.data[file] = nil
        end
    end
    for k, chatbox in ipairs(chatboxes) do
        kernel.modules.devfs.data["chatbox" .. chatbox:sub(1,4):upper()] = {
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
end

local function onComponentAdded(_, address, componentType)
    if componentType == "chat_box" then
        chatboxes[#chatboxes + 1] = address
        buildDevfs()
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "chat_box" then
        local t
        for i, chatbox in ipairs(chatboxes) do
            if chatbox == address then
                t = i
                break
            end
        end
        table.remove(chatboxes, t)
        buildDevfs()
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)

