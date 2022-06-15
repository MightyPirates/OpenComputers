local networks = require("networks")
local event = require("event")
local vcomponent = require("vcomponent")
local component = require("component")
local su = require("superUtiles")

-----------------------------------------

local appName = "netCP"

local function table_remove(tbl, obj)
    for i = 1, #tbl do
        if tbl[i] == obj then
            table.remove(tbl, i)
        end
    end
end

-----------------------------------------

local lib = {}

lib.hosted = {}
lib.connected = {}

function lib.host(network, name, uuid)
    local obj = {}
    obj.network = network
    obj.name = name
    obj.uuid = uuid
    obj.listens = {}
    obj.listens[#obj.listens] = event.register(nil, function(eventName, network_name, lappName, host_name, side, unicalCode, ...) --не table.insert чтоб было меньше скобок
        if eventName == "network_message" and network_name == obj.network.name and appName == lappName and host_name == obj.name then
            if side == "call" then
                obj.network.send(appName, obj.name, "return", unicalCode, pcall(component.invoke, obj.uuid, ...))
            end
        end
    end, math.huge, math.huge)
    obj.listens[#obj.listens] = event.register(nil, function(eventName, uuid, ...)
        if eventName then --так как eventName может быть nil что сведетельствует об отсутствии эвента
            if uuid == obj.uuid then
                if eventName == "component_removed" then
                    obj.kill()
                else
                    obj.network.send(appName, obj.name, "event", eventName, uuid, ...)
                end
            end
        end
    end, math.huge, math.huge)
    obj.listens[#obj.listens] = event.register(nil, function(eventName, network_name, lappName, host_name, side, unicalCode, ...) --не table.insert чтоб было меньше скобок
        if eventName == "network_message" and network_name == obj.network.name and appName == lappName and host_name == obj.name then
            if side == "check" then
                local doc = {}
                local methods = component.methods(obj.uuid)

                for k, v in pairs(methods) do
                    doc[k] = component.doc(obj.uuid, k)
                end

                obj.network.send(appName, obj.name, "returnCheck", unicalCode, component.type(obj.uuid), obj.uuid, methods, doc)
            end
        end
    end, math.huge, math.huge)

    function obj.kill()
        for i = 1, #obj.listens do
            event.cancel(obj.listens[i])
        end
        obj.network.send(appName, obj.name, "killed")
        table_remove(lib.hosted, obj)
    end

    table.insert(lib.hosted, obj)
    return obj
end

function lib.connect(network, name)
    local obj = {}
    obj.network = network
    obj.name = name

    obj.uuid = false
    obj.componentName = false
    obj.proxy = false
    
    obj.listens = {}
    obj.listens[#obj.listens] = event.register(nil, function(eventName, network_name, lappName, host_name, side, ...) --не table.insert чтоб было меньше скобок
        if eventName == "network_message" and network_name == obj.network.name and appName == lappName and host_name == obj.name then
            if side == "event" then
                event.push(...)
            elseif side == "killed" then
                obj.kill()
            end
        end
    end, math.huge, math.huge)

    local checkUnicalCode = su.generateRandomID()
    obj.network.send(appName, obj.name, "check", checkUnicalCode)
    obj.listens[#obj.listens] = event.register(nil, function(eventName, network_name, lappName, host_name, side, unicalCode, ...) --не table.insert чтоб было меньше скобок
        local args = {...}
        if eventName == "network_message" and network_name == obj.network.name and appName == lappName and host_name == obj.name and checkUnicalCode == unicalCode then
            if side == "returnCheck" then
                obj.uuid = args[2]
                obj.componentName = args[1]
                obj.doc = args[4]
                obj.proxy = {}
                for k, v in pairs(args[3]) do
                    obj.proxy[k] = function(...)
                        local unicalCallCode = su.generateRandomID()
                        obj.network.send(appName, obj.name, "call", unicalCallCode, k, ...)
                        local dat = {table.unpack({event.pull(4, "network_message", obj.network.name, appName, obj.name, "return", unicalCallCode)}, 7)}
                        if #dat == 0 then
                            error("no conection", 0)
                        end
                        if dat[1] then
                            return table.unpack(dat, 2)
                        else
                            error(dat[2] or "unkown", 0)
                        end
                    end
                end

                vcomponent.register(obj.uuid, obj.componentName, obj.proxy, obj.doc)
                return false
            end
        end
    end, math.huge, math.huge)
    

    function obj.kill()
        for i = 1, #obj.listens do
            event.cancel(obj.listens[i])
        end
        if obj.uuid then
            vcomponent.unregister(obj.uuid) 
        end
        table_remove(lib.connected, obj)
    end

    for i = 1, 15 do
        os.sleep(0.2)
        if obj.proxy and obj.componentName and obj.uuid then
            break
        end
    end
    if not obj.proxy then
        obj.kill()
        error("no conection")
    end

    table.insert(lib.connected, obj)
    return obj
end

function lib.getHost(name)
    for i = 1, #lib.hosted do
        if lib.hosted[i].name == name then
            return lib.hosted[i]
        end
    end
    return nil
end

function lib.getConnect(name)
    for i = 1, #lib.connected do
        if lib.connected[i].name == name then
            return lib.connected[i]
        end
    end
    return nil
end

return lib