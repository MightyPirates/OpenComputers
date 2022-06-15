local component = require("component")
local event = require("event")
local serialization = require("serialization")
local su = require("superUtiles")
local computer = require("computer")

------------------------------------------

------------------------------------------

local lib = {}
lib.devices = {}

function lib.create(address, maxPacketSize, dupProtectTimeout, dupProtectUpdateTimeout)
    local obj = {}
    obj.address = address
    obj.maxPacketSize = maxPacketSize or 7168
    obj.proxy = component.proxy(obj.address)
    obj.dupProtectTimeout = dupProtectTimeout or 8
    obj.dupProtectUpdateTimeout = dupProtectUpdateTimeout or 1
    obj.objCode = su.generateRandomID()
    obj.listens = {}

    obj.codes = {}
    obj.timeouts = {}
    local function addcode(code)
        if su.inTable(obj.codes, code) then
            return false
        else
            table.insert(obj.codes, code)
            table.insert(obj.timeouts, computer.uptime())
            return true
        end
    end

    table.insert(obj.listens, event.timer(obj.dupProtectUpdateTimeout, function()
        for i, code in pairs(obj.codes) do
            local timeout = obj.timeouts[i]
            if computer.uptime() - timeout > obj.dupProtectTimeout then
                table.remove(obj.codes, i)
                table.remove(obj.timeouts, i)
            end
        end
    end, math.huge))

    function obj.send(targetAddress, port, ...)
        local proxy = obj.proxy
        local sendData = serialization.serialize({...})
        local parts = su.toParts(sendData, obj.maxPacketSize)
        local randomCode = su.generateRandomID()

        for i, part in ipairs(parts) do
            local endflag = i == #parts
            local unicallCode = su.generateRandomID()
            addcode(unicallCode)
            if component.type(obj.address) == "tunnel" then
                proxy.send("bigModem", unicallCode, endflag, randomCode, i, part)
            else
                if targetAddress == true then
                    proxy.broadcast(port, "bigModem", unicallCode, endflag, randomCode, i, part)
                else
                    proxy.send(targetAddress, port, "bigModem", unicallCode, endflag, randomCode, i, part)
                end
            end
        end
    end

    function obj.broadcast(port, ...)
        obj.send(true, port, ...)
    end

    local buffer = {}
    table.insert(obj.listens, event.listen("modem_message", function(_, uuid, sender, port, dist, appName, unicallCode, endflag, randomCode, index, dat)
        if appName ~= "bigModem" or uuid ~= obj.address or not addcode(unicallCode) then return end
        if not buffer[randomCode] then buffer[randomCode] = {} end
        buffer[randomCode][index] = dat
        if endflag then
            local sendData = serialization.unserialize(table.concat(buffer[randomCode]))
            buffer[randomCode] = nil
            event.push("big_message", uuid, sender, port, dist, obj.objCode, su.unpack(sendData))
        end
    end))

    function obj.kill()
        for i = 1, #obj.listens do
            event.cancel(obj.listens[i])
        end
        table.remove(lib.devices, obj.indexInTable)
    end    

    table.insert(lib.devices, obj)
    obj.indexInTable = #lib.devices
    return obj
end

return lib