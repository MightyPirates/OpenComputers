local component = require("component")
local event = require("event")
local serialization = require("serialization")

-----------------------------------------

local function getModem()
    for address in component.list("modem") do
        local proxy = component.proxy(address)
        if proxy.isWireless() == true then
            return proxy
        end
    end
    error("wireless modem is not available")
end

-----------------------------------------

local lib = {}

local ok = false
lib.isOk = function()
    return ok
end

lib.raw_send = function(...)
    ok = false
    local modem = getModem()
    local port = math.random(1, 65535)
    local strength = modem.setStrength(8)
    local isOpen = modem.open(port)

    modem.broadcast(port, "nanomachines", "setResponsePort", port)
    local eventName = event.pull(4, "modem_message", modem.address, nil, port, nil, "nanomachines", "port", port)
    if not eventName then
        modem.setStrength(strength)
        if isOpen == true then modem.close(port) end
        error("no connection")
    end
    modem.broadcast(port, "nanomachines", ...)

    modem.setStrength(strength)
    local data = {event.pull(4, "modem_message", modem.address, nil, port, nil, "nanomachines")}
    if isOpen == true then modem.close(port) end
    if #data == 0 then error("no connection") end
    ok = true
    return table.unpack(data, 7)
end

--------------------

lib.getInput = function(num)
    checkArg(1, num, "number")
    local _, _, out = lib.raw_send("getInput", num)
    return out
end

lib.setInput = function(num, state)
    checkArg(1, num, "number")
    checkArg(2, state, "boolean")
    lib.raw_send("setInput", num, state)
end

lib.getActiveEffects = function()
    local _, out = lib.raw_send("getActiveEffects")
    --return serialization.unserialize(out)
    return out
end

lib.getMaxActiveInputs = function()
    local _, out = lib.raw_send("getMaxActiveInputs")
    return out
end

lib.getSafeActiveInputs = function()
    local _, out = lib.raw_send("getSafeActiveInputs")
    return out
end

lib.getTotalInputCount = function()
    local _, out = lib.raw_send("getTotalInputCount")
    return out
end

lib.getExperience = function()
    local _, out = lib.raw_send("getExperience")
    return out
end

lib.getName = function()
    local _, out = lib.raw_send("getName")
    return out
end

lib.getHunger = function()
    local _, out1, out2 = lib.raw_send("getHunger")
    return out1, out2
end

lib.getAge = function()
    local _, out = lib.raw_send("getAge")
    return out
end

lib.getHealth = function()
    local _, out = lib.raw_send("getHealth")
    return out
end

lib.getPowerState = function()
    local _, out1, out2 = lib.raw_send("getPowerState")
    return out1, out2
end

lib.saveConfiguration = function()
    local _, out1, out2 = lib.raw_send("saveConfiguration")
    return out1, out2
end

return lib