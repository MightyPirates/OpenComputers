--[[
Communication on port 1!
Node protocol:
Hello/broadcast(sent by new host in node):  \0 (modem addersses are in event)
Hi/direct(sent by hosts to new host):       \1 (^)
OHAI/direct(Ack of Hi(\1))                  \2
Host quitting/broadcast                     \3 (^)
Data/direct                                 \4[data] (origin from event)
]]
local driver = {}

local nodes = {}
local eventHnd

local PKT_BEACON        = "\32"
local PKT_REGISTER      = "\1"
local PKT_REGISTER_ACK  = "\2"
local PKT_QUIT          = "\3"
local PKT_DATA          = "\4"

function driver.start(eventHandler)
    eventHnd = eventHandler

    eventHandler.setListener("modem_message", function(_, interface, origin, port, _, data)
            if not nodes[interface] then return end --other kind of modem(possibly tunnel)
            
            eventHandler.debug("modemmsg["..nodes[interface].name.."]/"..origin..":"..data)
            
            nodes[interface].pktIn = nodes[interface].pktIn + 1
            nodes[interface].bytesIn = nodes[interface].bytesIn + data:len()
            
            if data:sub(1,1) == PKT_BEACON then
                component.invoke(interface, "send", origin, 1, PKT_REGISTER )
            elseif data:sub(1,1) == PKT_REGISTER then
                eventHandler.newHost(nodes[interface].name, origin)
                component.invoke(interface, "send", origin, 1, PKT_REGISTER_ACK)
            elseif data:sub(1,1) == PKT_REGISTER_ACK then
                eventHandler.newHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == PKT_QUIT then
                eventHandler.delHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == PKT_DATA then
                eventHandler.recvData(data:sub(2), nodes[interface].name, origin)
            end
            
        end)
    
    eventHandler.setListener("component_added", function(_, int, ctype)
        if ctype ~= "modem" then return end
        local name = "eth" .. int:sub(1, 4):upper()
        eventHandler.newInterface(name, int, "Ethernet")
        
        nodes[name] = {modem = int, name = name, pktIn = 0, pktOut = 1, bytesIn = 0, bytesOut = 1}
        nodes[int] = nodes[name]
        
        component.invoke(int, "open", 1)
        component.invoke(int, "broadcast", 1, PKT_BEACON)
        
        eventHandler.newHost(name, int)--register loopback
    end)
    
    eventHandler.setListener("component_removed", function(_, int, ctype)
        if ctype ~= "modem" then return end
        local name = "eth" .. int:sub(1, 4):upper()
        nodes[name] = nil
        nodes[int] = nil
        eventHnd.delInterface(int)
    end)
    return {}
end

function driver.send(handle, interface, destination, data)
    if nodes[interface] then
        if nodes[interface].modem == destination then
            nodes[interface].pktOut = nodes[interface].pktOut + 1
            nodes[interface].bytesOut = nodes[interface].bytesOut + data:len()
            nodes[interface].pktIn = nodes[interface].pktIn + 1
            nodes[interface].bytesIn = nodes[interface].bytesIn + data:len()
            eventHnd.recvData(data, interface, destination)
        else
            nodes[interface].pktOut = nodes[interface].pktOut + 1
            nodes[interface].bytesOut = nodes[interface].bytesOut + 1 + data:len()
            component.invoke(nodes[interface].modem, "send", destination, 1, PKT_DATA..data)
        end
    end
end

function driver.info(interface)
    if nodes[interface] then
        return nodes[interface].pktIn,nodes[interface].pktOut,nodes[interface].bytesIn,nodes[interface].bytesOut
    end
    return 0,0,0,0
end

return driver