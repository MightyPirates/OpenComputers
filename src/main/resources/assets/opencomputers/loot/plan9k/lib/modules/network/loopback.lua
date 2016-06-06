local driver = {}

local pktIn,pktOut,bytesIn,bytesOut = 0,0,0,0

function driver.start(eventHandler)
    eventHandler.newInterface("lo", "localhost", "Local Loopback")
    eventHandler.newHost("lo", "localhost")
    return {send = function(data)eventHandler.recvData(data, "lo", "localhost")end}
end

function driver.send(handle, interface, destination, data)
    if interface == "lo" and destination == "localhost" then
        pktIn, pktOut = pktIn+1,pktOut+1
        bytesIn,bytesOut = bytesIn + data:len(), bytesOut + data:len() 
        handle.send(data)
    end
end

function driver.info(interface)
    if interface == "lo" then
        return pktIn,pktOut,bytesIn,bytesOut
    end
    return 0,0,0,0
end

return driver
