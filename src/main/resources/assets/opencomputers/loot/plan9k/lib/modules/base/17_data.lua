local block = kernel.modules.block

local function buildDevice(addr)
    return {
        __type = "f",
        read = function(h, len)
            return component.invoke(addr, "random", len)
        end
    }
end

local function onComponentAdded(_, address, componentType)
    if componentType == "data" then
        block.register(address, "urandom", buildDevice(address))
    end
end

local function onComponentRemoved(_, address, componentType)
    if componentType == "data" then
        block.unregister(address)
    end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
