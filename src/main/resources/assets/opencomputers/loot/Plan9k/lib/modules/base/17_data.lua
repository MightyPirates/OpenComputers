cards = {}


local function buildDevfs()
    for file in pairs(kernel.modules.devfs.data) do
        if file == "urandom" then
            kernel.modules.devfs.data[file] = nil
        end
    end

    kernel.modules.devfs.data["urandom"] = {
        __type = "f",
        read = function(h, len)
            return component.invoke(cards[1], "random", len)
        end
    }
end

local function onComponentAdded(_, address, componentType)
    if componentType == "data" then
        cards[#cards + 1] = address
        buildDevfs()
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "data" then
        local t
        for i, card in ipairs(cards) do
            if card == address then
                t = i
                break
            end
        end
        table.remove(cards, t)
        buildDevfs()
    end
end

--function start()
    --for card, t in component.list("data") do
    --    onComponentAdded(_, card, t)
    --end
--end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
