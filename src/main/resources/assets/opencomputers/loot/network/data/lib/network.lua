local event = require "event"
local computer = require "computer"

local driver = {}

local network = {}
local internal = {}

------------
--Core communication
network.core = {}

function network.core.setCallback(name, fn)
    driver[name] = fn
end

function network.core.lockCore()
    network.core = nil
end

------------
--ICMP

network.icmp = {}
internal.icmp = {}

local pingid = 0
function network.icmp.ping(addr, payload)
    pingid = pingid + 1
    driver.send(addr, "IP"..computer.address()..":"..tostring(pingid)..":"..payload)
    return pingid
end

function internal.icmp.handle(origin, data)
    if data:sub(2,2) == "P" then
        local matcher = data:sub(3):gmatch("[^:]+")
        local compid = matcher()
        local id = tonumber(matcher())
        local payload = matcher()
        if compid == computer.address() then
            computer.pushSignal("ping_reply", origin, tonumber(id), payload)
        else
            driver.send(origin, data)
        end
    end
end

------------
--Datagrams - UDP like protocol

network.udp = {}
internal.udp = {ports = {}}

function internal.udp.checkPortRange(port)
    if port < 0 or port > 65535 then error("Wrong port!")end
end

function network.udp.open(port)
    internal.udp.checkPortRange(port)
    internal.udp.ports[port] = true
end

function network.udp.close(port)
    internal.udp.checkPortRange(port)
    internal.udp.ports[port] = nil
end

function network.udp.send(addr, port, data)
    internal.udp.checkPortRange(port)
    driver.send(addr, "D".. string.char(math.floor(port/256))..string.char(port%256)..data)
end

function internal.udp.handle(origin, data)
    local port = data:byte(2)*256 + data:byte(3)
    if internal.udp.ports[port] then
        computer.pushSignal("datagram", origin, port, data:sub(4))
    end
end

-----------
--TCP - TCP like protocol

--O[port,2B][openers channel,2B] --Try open connection
--A[opened channel,2B][openers channel,2B] --Accept connection
--R[openers channel,2B] --Reject connection i.e. closed port
--C[remote channel,2B] --Close connection(user request or adta at closed/wrong channel)
--D[remote channel,2B][data] --Data

network.tcp = {}
internal.tcp = {ports = {}, channels = {}, freeCh = 1}

function network.tcp.listen(port)
    internal.udp.checkPortRange(port)
    internal.tcp.ports[port] = true
    
end

function network.tcp.unlisten(port)
    internal.udp.checkPortRange(port)
    internal.tcp.ports[port] = nil
end

function network.tcp.open(addr, port)
    internal.udp.checkPortRange(port)
    local ch = internal.tcp.freeCh
    if internal.tcp.channels[ch] and internal.tcp.channels[ch].next then 
        internal.tcp.freeCh = internal.tcp.channels[ch].next
    else
        internal.tcp.freeCh = #internal.tcp.channels+2
    end
    internal.tcp.channels[ch] = {open = false, waiting = true, addr = addr, port = port}--mark openning
    
    driver.send(addr, "TO".. string.char(math.floor(port/256))..string.char(port%256).. string.char(math.floor(ch/256))..string.char(ch%256))
    return ch
end

function network.tcp.close(channel)
    if internal.tcp.channels[channel] then
        if internal.tcp.channels[channel].open or internal.tcp.channels[channel].waiting then
            driver.send(internal.tcp.channels[channel].addr, "TC"..  string.char(math.floor(internal.tcp.channels[channel].remote/256))..string.char(internal.tcp.channels[channel].remote%256))
        end
        internal.tcp.channels[channel] = {next = internal.tcp.freeCh}
        internal.tcp.freeCh = channel
        --computer.pushSignal("tcp_close", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
    end
end

function network.tcp.send(channel, data)
    if internal.tcp.channels[channel] and internal.tcp.channels[channel].open then
        driver.send(internal.tcp.channels[channel].addr, "TD".. string.char(math.floor(internal.tcp.channels[channel].remote/256))..string.char(internal.tcp.channels[channel].remote%256)..data)
        return true
    end
    return false
end

function internal.tcp.handle(origin, data)
    if data:sub(2,2) == "O" then
        local port = data:byte(3)*256 + data:byte(4)
        local rchan = data:byte(5)*256 + data:byte(6)
        
        if internal.tcp.ports[port] then
            local ch = internal.tcp.freeCh
            if internal.tcp.channels[ch] and internal.tcp.channels[ch].next then 
                internal.tcp.freeCh = internal.tcp.channels[ch].next
            else
                internal.tcp.freeCh = #internal.tcp.channels+2
            end
            internal.tcp.channels[ch] = {open = true, remote = rchan, addr = origin,  port = port}
            driver.send(origin, "TA".. string.char(math.floor(ch/256))..string.char(ch%256) .. string.char(math.floor(rchan/256)) .. string.char(rchan%256))
            computer.pushSignal("tcp", "connection", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
        else
            driver.send(origin, "TR".. string.char(math.floor(rchan/256))..string.char(rchan%256))
        end
    elseif data:sub(2,2) == "R" then
        local ch = data:byte(3)*256 + data:byte(4)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].waiting then
            internal.tcp.channels[ch] = {next = internal.tcp.freeCh}
            internal.tcp.freeCh = ch
        end
    elseif data:sub(2,2) == "A" then
        local remote = data:byte(3)*256 + data:byte(4)
        local ch = data:byte(5)*256 + data:byte(6)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].waiting then
            internal.tcp.channels[ch].waiting = nil
            internal.tcp.channels[ch].open = true
            internal.tcp.channels[ch].remote = remote
            computer.pushSignal("tcp", "connection", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
        end
    elseif data:sub(2,2) == "C" then
        local ch = data:byte(3)*256 + data:byte(4)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].open then
            internal.tcp.channels[ch] = {next = internal.tcp.freeCh}
            internal.tcp.freeCh = ch
            computer.pushSignal("tcp" ,"close", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
        end
    elseif data:sub(2,2) == "D" then
        local ch = data:byte(3)*256 + data:byte(4)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].open then
            computer.pushSignal("tcp", "message", ch, data:sub(5), internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
        end
    end
end

-----------
--IP

network.ip = {}

function network.ip.bind(addr)
    driver.bind(addr)
end

------------
--Data processing

event.listen("network_message", function(_, origin, data)
    --print("NETMSG/",origin,data)
    if data:sub(1,1) == "I" then internal.icmp.handle(origin, data)
    elseif data:sub(1,1) == "T" then internal.tcp.handle(origin, data)
    elseif data:sub(1,1) == "D" then internal.udp.handle(origin, data) end
        
end)


------------
--Info

network.info = {}
network.info.getInfo = function(...)return driver.netstat(...) end
network.info.getInterfaceInfo = function(...)return driver.intstat(...) end
network.info.getRoutes = function(...)return driver.routetab(...) end
network.info.getArpTable = function(...)return driver.arptab(...) end

------------

return network

