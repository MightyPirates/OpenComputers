--[[

Communication on port 1!

Node protocol:

Hello/broadcast(sent by new host in node):  H (modem addersses are in event)
Hi/direct(sent by hosts to new host):       I (^)
Host quitting/broadcast                     Q (^)
Data/direct                                 D[data] (origin from event)

]]
local component = require "component"
local event = require "event"

local driver = {}

local nodes = {}
local eventHnd

function driver.start(eventHandler)
    eventHnd = eventHandler
    local c = 0
    
    eventHandler.setListener("modem_message", function(_, interface, origin, port, _, data)
            if not nodes[interface] then return end --other kind of modem(possibly tunnel)
            
            eventHandler.debug("modemmsg["..nodes[interface].name.."]/"..origin..":"..data)
            
            nodes[interface].pktIn = nodes[interface].pktIn + 1
            nodes[interface].bytesIn = nodes[interface].bytesIn + data:len()
            
            if data:sub(1,1) == "H" then
                eventHandler.newHost(nodes[interface].name, origin)
                component.invoke(interface, "send", origin, 1, "I")
                eventHandler.debug("REPL:",interface,origin)
            elseif data:sub(1,1) == "I"then
                eventHandler.newHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == "Q"then
                eventHandler.delHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == "D"then
                eventHandler.recvData(data:sub(2), nodes[interface].name, origin)
            end
            
        end)
    
    for int in component.list("modem", true)do
        eventHandler.newInterface("eth"..tostring(c), int, "Ethernet")
        
        nodes["eth"..tostring(c)] = {modem = int, name = "eth"..tostring(c), pktIn = 0, pktOut = 1, bytesIn = 0, bytesOut = 1}
        nodes[int] = nodes["eth"..tostring(c)]
        
        component.invoke(int, "open", 1)
        component.invoke(int, "broadcast", 1, "H")
        
        eventHandler.newHost("eth"..tostring(c), int)--register loopback
        c = c + 1
    end
    
    --eventHandler.newHost("lo", "localhost")
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
            component.invoke(nodes[interface].modem, "send", destination, 1, "D"..data)
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
