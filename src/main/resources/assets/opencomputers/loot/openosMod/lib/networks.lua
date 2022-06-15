local component = require("component")
local event = require("event")
local serialization = require("serialization")
local su = require("superUtiles")
local computer = require("computer")
local bigModem = require("bigModem")

-------------------------------------------

local noAddress
local function raw_send(devices, name, code, data, obj, isResend, port)
    local noAddress2 = noAddress
    noAddress = nil
    for i = 1, #devices do
        local device = devices[i]
        if isResend then
            if device["resend"] == nil then
                if not obj.resend then
                    goto skip
                end
            elseif device["resend"] == false then
                goto skip
            end
        end
        local proxy = component.proxy(device[1])
        if proxy.type == "modem" then
            if isResend and proxy.address == noAddress2 and device[2] == port and (not proxy.isWireless() or device[3] == 0) then
                goto skip
            end
            local strength = device[3]
            local oldStrength
            if proxy.isWireless() then
                if strength then
                    oldStrength = proxy.getStrength()
                    proxy.setStrength(strength)
                end
            end

            device.bigModem.broadcast(device[2], "network", name, code, data)

            if oldStrength then proxy.setStrength(oldStrength) end
        elseif proxy.type == "tunnel" then
            if not isResend or proxy.address ~= noAddress2 then
                device.bigModem.broadcast(0, "network", name, code, data)
            end
        else
            error("unsupported device")
        end
        ::skip::
    end
end

local function isType(data, target)
    return type(data) == target
end

-------------------------------------------

local lib = {}

lib.networks = {}

function lib.create(devices, name, resend)
    checkArg(1, devices, "table")
    checkArg(2, name, "string")
    local obj = {}
    obj.devices = devices
    obj.name = name
    obj.resend = resend
    obj.listens = {}

    --------------------------------------------------

    for i = 1, #obj.devices do
        local device = obj.devices[i]
        local proxy = component.proxy(device[1])
        device.bigModem = bigModem.create(device[1])
        if proxy.type == "modem" then
            device.isOpen = proxy.open(device[2])
        end
    end

    --------------------------------------------------

    local messagebuffer = {}
    local life = {}

    local function cleanBuffer()
        for key, value in pairs(life) do
            if computer.uptime() - value > 8 then
                messagebuffer[key] = nil
                life[key] = nil
            end
        end
    end
    obj.listens[#obj.listens + 1] = event.timer(1, cleanBuffer, math.huge)

    local function addcode(code)
        local index = su.generateRandomID()
        messagebuffer[index] = code or su.generateRandomID()
        life[index] = computer.uptime()
        return messagebuffer[index]
    end

    local function listen(_, this, _, port, _, objCode, messagetype, name, code, data)
        if not isType(messagetype, "string") or not isType(name, "string") or not isType(code, "string") then return end
        if su.inTable(messagebuffer, code) or name ~= obj.name or messagetype ~= "network" then return end
        local ok = false
        local device
        for i = 1, #obj.devices do
            device = obj.devices[i]
            if device[1] == this and (port == 0 or device[2] == port) and device.bigModem.objCode == objCode then
                ok = true
                break
            end
        end
        if not ok then return end
        addcode(code)
        local function resendPack()
            noAddress = this
            raw_send(obj.devices, obj.name, code, data, obj, true, port)
        end
        if device["resend"] == nil then
            if obj.resend then
                resendPack()
            end
        elseif device["resend"] == true then
            resendPack()
        end
        local out = serialization.unserialize(data)
        event.push("network_message", obj.name, su.unpack(out))
    end
    table.insert(obj.listens, event.listen("big_message", listen))

    --------------------------------------------------

    function obj.send(...)
        local data = serialization.serialize({...})
        raw_send(obj.devices, obj.name, addcode(), data, obj)
    end

    lib.networks[#lib.networks + 1] = obj
    local thisIndex = #lib.networks

    function obj.kill()
        for i = 1, #obj.listens do event.cancel(obj.listens[i]) end
        for i = 1, #obj.devices do
            local device = obj.devices[i]
            device.bigModem.kill()
            if device["isOpen"] then
                component.proxy(device[1]).close(device[2])
            end
        end
        table.remove(lib.networks, thisIndex)
    end

    return obj
end

function lib.getDevices(tunnels, modems, wiredModems, wirelessModems, modemsPort, modemsStrength)
    if not modemsPort then modemsPort = 88 end
    if not modemsStrength then modemsStrength = math.huge end

    ------------------------------------------------------

    local devices = {}

    if tunnels then
        for address in component.list("tunnel") do
            devices[#devices + 1] = {address}
        end
    end
    if wiredModems then
        for address in component.list("modem") do
            if component.invoke(address, "isWired") and not component.invoke(address, "isWireless") then
                devices[#devices + 1] = {address, modemsPort, modemsStrength}
            end
        end
    end
    if wirelessModems then
        for address in component.list("modem") do
            if not component.invoke(address, "isWired") and component.invoke(address, "isWireless") then
                devices[#devices + 1] = {address, modemsPort, modemsStrength}
            end
        end
    end
    if modems then
        for address in component.list("modem") do
            devices[#devices + 1] = {address, modemsPort, modemsStrength}
        end
    end

    return devices
end

function lib.getNetwork(name)
    for i = 1, #lib.networks do
        if lib.networks[i].name == name then
            return lib.networks[i]
        end
    end
end

return lib