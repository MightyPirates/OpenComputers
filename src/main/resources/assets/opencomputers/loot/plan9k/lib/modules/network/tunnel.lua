--For protocol info look to at modem driver

local driver = {}

local nodes = {}
local eventHnd

function driver.start(eventHandler)
    eventHnd = eventHandler
    
    eventHandler.setListener("modem_message", function(_, interface, origin, port, _, data)
            if not nodes[interface] then return end --other kind of modem(possibly modem)
            
            eventHandler.debug("modemmsg["..nodes[interface].name.."]/"..origin..":"..data)
            
            nodes[interface].pktIn = nodes[interface].pktIn + 1
            nodes[interface].bytesIn = nodes[interface].bytesIn + data:len()
            
            if data:sub(1,1) == "H" then
                eventHandler.newHost(nodes[interface].name, origin)
                component.invoke(interface, "send", "I")
                eventHandler.debug("REPL:",interface,origin)
            elseif data:sub(1,1) == "I"then
                eventHandler.newHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == "Q"then
                eventHandler.delHost(nodes[interface].name, origin)
            elseif data:sub(1,1) == "D"then
                eventHandler.recvData(data:sub(2), nodes[interface].name, origin)
            end
            
        end)
    
    for int in component.list("tunnel", true)do
        eventHandler.newInterface("tun" .. int:sub(1, 4):upper(), int, "Tunnel")
        
        nodes["tun" .. int:sub(1, 4):upper()] = {modem = int, name = "tun" .. int:sub(1, 4):upper(), pktIn = 0, pktOut = 1, bytesIn = 0, bytesOut = 1}
        nodes[int] = nodes["tun" .. int:sub(1, 4):upper()]
        
        component.invoke(int, "send", "H")
        
        eventHandler.newHost("tun" .. int:sub(1, 4):upper(), int)--register loopback
    end
    
    --eventHandler.newInterface("lo", "localhost", "Local Loopback")
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
            component.invoke(nodes[interface].modem, "send", "D"..data)
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
