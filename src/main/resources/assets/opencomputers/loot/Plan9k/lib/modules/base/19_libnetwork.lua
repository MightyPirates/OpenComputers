--local event = require "event"

local driver

network = {}
internal = {}


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
    if not port then
        port = 49151 + math.floor(math.random() * 16384)
        --TODO: check allocated port
    end
    internal.udp.checkPortRange(port)
    if kernel.modules.threading.currentThread then
        internal.udp.ports[port] = kernel.modules.threading.currentThread.pid
    else
        internal.udp.ports[port] = true
    end
    return port
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

function internal.udp.cleanProcess(thread)
    for port, pid in pairs(internal.udp.ports) do
        if pid == thread.pid then
            internal.udp.ports[port] = nil
        end
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
    if kernel.modules.threading.currentThread then
        internal.tcp.ports[port] = kernel.modules.threading.currentThread.pid
    else
        internal.tcp.ports[port] = true
    end
end

function network.tcp.unlisten(port)
    internal.udp.checkPortRange(port)
    internal.tcp.ports[port] = nil
end

function network.tcp.open(addr, port)
    if not port then
        port = 49151 + math.floor(math.random() * 16384)
        --TODO: check allocated port
    end
    internal.udp.checkPortRange(port)
    local ch = internal.tcp.freeCh
    if internal.tcp.channels[ch] and internal.tcp.channels[ch].next then 
        internal.tcp.freeCh = internal.tcp.channels[ch].next
    else
        internal.tcp.freeCh = #internal.tcp.channels+2
    end
    --kernel.io.println("TCP: use ch " .. ch)
    internal.tcp.channels[ch] = {open = false, waiting = true, addr = addr, port = port}--mark openning
    if kernel.modules.threading.currentThread then
        internal.tcp.channels[ch].owner = kernel.modules.threading.currentThread.pid
    end
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
            --kernel.io.println("TCP: use ch " .. ch)
            internal.tcp.channels[ch] = {open = true, remote = rchan, addr = origin,  port = port}
            if type(internal.tcp.ports[port]) == "number" then
                internal.tcp.channels[ch].owner = internal.tcp.ports[port]
            end
            driver.send(origin, "TA".. string.char(math.floor(ch/256))..string.char(ch%256) .. string.char(math.floor(rchan/256)) .. string.char(rchan%256))
            computer.pushSignal("tcp", "connection", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port, "incoming")
        else
            driver.send(origin, "TR".. string.char(math.floor(rchan/256))..string.char(rchan%256))
        end
    elseif data:sub(2,2) == "R" then
        local ch = data:byte(3)*256 + data:byte(4)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].waiting then
            computer.pushSignal("tcp" ,"close", ch, internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
            internal.tcp.channels[ch] = {next = internal.tcp.freeCh}
            internal.tcp.freeCh = ch
        end
    elseif data:sub(2,2) == "A" then
        local remote = data:byte(3)*256 + data:byte(4)
        local ch = data:byte(3)*256 + data:byte(4)
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
    elseif data:sub(2,2) == "D" then --TODO: check source
        local ch = data:byte(3)*256 + data:byte(4)
        if internal.tcp.channels[ch] and internal.tcp.channels[ch].open then
            computer.pushSignal("tcp", "message", ch, data:sub(5), internal.tcp.channels[ch].addr, internal.tcp.channels[ch].port)
        end
    end
end

function internal.tcp.cleanup()
    for channel, _ in pairs(internal.tcp.channels) do
        if internal.tcp.channels[channel].open or internal.tcp.channels[channel].waiting then
            if internal.tcp.channels[channel].open or internal.tcp.channels[channel].waiting then
                driver.send(internal.tcp.channels[channel].addr, "TC"..  string.char(math.floor(internal.tcp.channels[channel].remote/256))..string.char(internal.tcp.channels[channel].remote%256))
            end
            internal.tcp.channels[channel] = {next = internal.tcp.freeCh}
            internal.tcp.freeCh = channel
        end
    end
end

function internal.tcp.cleanProcess(thread)
    for channel, _ in pairs(internal.tcp.channels) do
        if internal.tcp.channels[channel].owner == thread.pid then
            network.tcp.close(channel)
        end
    end
    for port, pid in pairs(internal.tcp.ports) do
        if pid == thread.pid then
            internal.tcp.ports[port] = nil
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

function handleData(origin, data)
    if data:sub(1,1) == "I" then internal.icmp.handle(origin, data)
    elseif data:sub(1,1) == "T" then internal.tcp.handle(origin, data)
    elseif data:sub(1,1) == "D" then internal.udp.handle(origin, data) end
end


------------
--Info

network.info = {}
network.info.getInfo = function(...)return driver.netstat(...) end
network.info.getInterfaceInfo = function(...)return driver.intstat(...) end
network.info.getRoutes = function(...)return driver.routetab(...) end
network.info.getArpTable = function(...)return driver.arptab(...) end

------------

kernel.userspace.package.preload.network = network

function start()
    driver = {
        send = kernel.modules.network.send,
        bind = kernel.modules.network.bindAddr,
        netstat = kernel.modules.network.getInfo,
        intstat = kernel.modules.network.getInterfaceInfo,
        routetab = kernel.modules.network.getRoutingTable,
        arptab = kernel.modules.network.getArpTable,
    }
    
    kernel.modules.gc.onShutdown(function()
        internal.tcp.cleanup()
    end)
    
    kernel.modules.gc.onProcessKilled(function(thread)
        internal.tcp.cleanProcess(thread)
        internal.udp.cleanProcess(thread)
    end)
end
