local event = require "event"
local computer = require "computer"
local network = require "network"

local _rawSend
local isAccessible
local getNodes
local getInterfaceInfo
local startNetwork

local dataHandler --Layer 2 data handler

local accessibleHosts
local nodes

------------------------
--Layer 1

local initated = false

local function start()
    if initated then return end
    initated = true
    
    local computer = require "computer"
    local filesystem = require "filesystem"
    --local posix = require "posix"
    
    ---------
    --Debug
    --posix.verbose(print)
    ---------
    
    
    accessibleHosts = {}
    nodes = {}
    
    --DRIVER INIT
    --print("Loading drivers")

    local drivers = {}

    for file in filesystem.list("/lib/network") do
        
        --print("Loading driver:", file)
        drivers[file] = {driver = loadfile("/lib/network/"..file)()}
        
        local eventHandler = {}--EVENT HANDLERS FOR DRIVER EVENTS
        --eventHandler.debug = print
        eventHandler.debug = function()end
        
        function eventHandler.newHost(node, address)--New interface in net node
            --print("New host: ",node, address)
            accessibleHosts[address] = {driver = drivers[file], node = node}
            nodes[node].hosts[address] = address--mark host in node
        end
        
        function eventHandler.newInterface(interface, selfAddr, linkName)--New node
            --print("New interface: ",interface, selfaddr)
            nodes[interface] = {hosts={}, driver = drivers[file], selfAddr = selfAddr, linkName = linkName}
        end
        
        function eventHandler.recvData(data, node, origin)
            dataHandler(data, node, origin)
        end
        
        function eventHandler.setListener(evt, listener)
            return event.listen(evt, function(...)
                local args = {...}
                local res = {pcall(function()listener(table.unpack(args))end)}
                if not res[1] then
                    print("ERROR IN NET EVENTHANDLER["..file.."]:",res[2])
                end
                return table.unpack(res,2)
            end)
        end
        
        drivers[file].handle = drivers[file].driver.start(eventHandler)
    end
    
    _rawSend = function(addr, node, data)
        --print("TrySend:",node,addr,":",data)
        if accessibleHosts[addr] then
            accessibleHosts[addr].driver.driver.send(accessibleHosts[addr].driver.handle, node, addr, data)
        end
    end

    isAccessible = function(addr)
        if not accessibleHosts[addr] then return end
        return accessibleHosts[addr].node, accessibleHosts[addr].driver
    end
    
    getNodes = function()
        return nodes
    end
    
    getInterfaceInfo = function(interface)
        if nodes[interface] then
            return nodes[interface].driver.driver.info(interface)
        end
    end
    
    print("Link Control initated")
    startNetwork()
    print("Network initated")
    computer.pushSignal("network_ready")
end

------------------------
--Layer 2

startNetwork  = function()
    
    local ttl = 32
    local rawSend
    local send

    local routeRequests = {} -- Table by dest addressed of tables {type = T[, data=..]}, types: D(own waiting data), R(route request for someone), E(routed data we should be able to route..)
    local routes = {} --Table of pairs -> [this or route] / {thisHost=true} / {router = [addr]}
    
    routes[computer.address()] = {thisHost=true}
    
    -----Utils
    local function sizeToString(size)
        return string.char((size)%256) .. string.char(math.floor(size/256)%256) .. string.char(math.floor(size/65536)%256)
    end

    local function readSizeStr(str, pos)
        local len = str:sub(pos,pos):byte()
        return str:sub(pos+1, pos+len), len+1
    end

    local toByte = string.char
    -----Data out
    
    local function onRecv(origin, data)
        computer.pushSignal("network_message", origin, data)
    end
    
    -----Sending
    
    local function sendDirectData(addr, data)--D[ttl-byte][data]
        return rawSend(addr, "D"..toByte(ttl)..data)
    end
    
    local function sendRoutedData(addr, data)--E[ttl-byte][hostlen-byte][dest host][hostlen-byte][origin host]message
        local nodes = getNodes()
        local msg = "E"..toByte(ttl)..toByte(addr:len())..addr..toByte(nodes[routes[addr].node].selfAddr:len())..nodes[routes[addr].node].selfAddr..data
        _rawSend(routes[addr].router, routes[addr].node, msg)
    end
    
    local function sendRoutedDataAs(addr, origin, data, ottl)--E[ttl-byte][hostlen-byte][dest host][hostlen-byte][origin host]message
        local msg = "E"..toByte(ottl-1)..toByte(addr:len())..addr..toByte(origin:len())..origin..data
        _rawSend(routes[addr].router, routes[addr].node, msg)
    end
    
    local function sendRouteRequest(addr)--R[ttl-byte][Addr len][Requested addr][Route hosts-byte][ [addrLen-byte][addr] ]
        local base = "R"..toByte(ttl)..toByte(addr:len())..addr..toByte(1)
        local nodes = getNodes()
        local sent = {}
        for node, n in pairs(nodes) do
            for host in pairs(n.hosts)do
                if not sent[host]then
                    sent[host] = true
                    _rawSend(host, node, base..toByte(n.selfAddr:len())..n.selfAddr)
                end
            end
        end
        sent = nil
    end
    
    local function resendRouteRequest(orig, node, host, nttl)--R[ttl-byte][Addr len][Requested addr][Route hosts-byte][ [addrLen-byte][addr] ]
        local nodes = getNodes()
        local hlen = orig:sub(3,3):byte()
        
        --local msg = "R" .. toByte(nttl) .. toByte(hlen+1) .. orig:sub(pos+4) .. toByte(nodes[node].selfAddr) .. nodes[node].selfAddr --broken, TODO repair
        local msg = "R" .. toByte(nttl) .. orig:sub(3) --workaround
        _rawSend(host, node, msg)
    end
    
    local function sendHostFound(dest, addr)--H[ttl-byte][Found host]
        return rawSend(dest, "H"..toByte(ttl)..addr)
    end
    
    rawSend = function(addr, data)
        local node, driver = isAccessible(addr)
        if node then
            _rawSend(addr, node, data)
            return true
        end
        return false
    end
    
    send = function(addr, data)
        if type(addr) ~= "string" then error("Address must be string!!") end
        if not sendDirectData(addr, data) then--Try send directly
            if routes[addr] then
                if routes[addr].thisHost then
                    onRecv("localhost", data)--it's this host, use loopback
                else
                    sendRoutedData(addr, data)--We know route, try to send it that way
                end
            else
                --route is unknown, we have to request it if we haven't done so already
                if not routeRequests[addr] then 
                    routeRequests[addr] = {}
                    routeRequests[addr][#routeRequests[addr]+1] = {type = "D", data = data}
                    sendRouteRequest(addr)
                else
                    routeRequests[addr][#routeRequests[addr]+1] = {type = "D", data = data}
                end
            end
        end
    end
    
    local function processRouteRequests(host)
        if routeRequests[host] then
            for _, request in pairs(routeRequests[host]) do
                if request.type == "D" then
                    sendRoutedData(host, request.data)
                elseif request.type == "E" then
                    if request.ttl-1 > 1 then
                        sendRoutedDataAs(host, request.origin, request.data, request.ttl)
                    end
                elseif request.type == "R" then
                    sendHostFound(request.host, host)
                end
            end
            routeRequests[host] = nil
        end
    end
    
    local function checkRouteDest(dest, origin, node, data)
        local nodes = getNodes()
        if dest == nodes[node].selfAddr then
            return true
        elseif routes[dest] and routes[dest].thisHost then
            return true
        end
        return false
    end
    
    bindAddr = function(addr)
        routes[addr] = {thisHost=true}
        processRouteRequests(addr)
    end
    
    dataHandler = function(data, node, origin)
        --print("DATA:", data, node, origin)
        
        if data:sub(1,1) == "D" then --Direct data
            onRecv(origin, data:sub(3))
        elseif data:sub(1,1) == "E" then --Routed data
            local ttl = data:byte(2)
            local dest, destlen = readSizeStr(data, 3)
            local orig, origlen = readSizeStr(data, 3+destlen)
            local dat = data:sub(3+destlen+origlen)
            if checkRouteDest(dest, orig, node, dat) then
                onRecv(orig, dat)
            else
                if routes[dest] then
                    if ttl-1 > 0 then
                        sendRoutedDataAs(dest, orig, dat, ttl)
                    end
                else
                    local _node, driver = isAccessible(dest)
                    if _node then
                        routes[dest] = {router = dest, node = _node}
                        if ttl-1 > 0 then
                            sendRoutedDataAs(dest, orig, dat, ttl)
                        end
                    else
                        if not routeRequests[dest] then routeRequests[dest] = {} end
                        routeRequests[dest][#routeRequests[dest]+1] = {type = "E", origin = orig, ttl = ttl, data = dat}
                        sendRouteRequest(dest)
                    end
                end
            end
        elseif data:sub(1,1) == "R" then --Route request
            local dest, l = readSizeStr(data, 3)
            if not routeRequests[dest] then
                
                --check if accessible interface
                local nodes = getNodes()
                for _node, n in pairs(nodes) do
                    if _node ~= node then --requested host won't ever be in same node
                        for host in pairs(n.hosts)do
                            if host == dest then
                                --Found it!
                                sendHostFound(origin, dest)
                                return
                            end
                        end
                    end
                end
                
                --check if route known
                if routes[dest] then
                    if routes[dest].thisHost then
                        --sendHostFound(origin, nodes[node].selfAddr)
                        sendHostFound(origin, dest)
                    elseif routes[dest].router ~= origin then--Routen might have rebooted and is asking about route
                        --sendHostFound(origin, routes[dest].router)
                        sendHostFound(origin, dest)
                    end
                    return
                end
                
                routeRequests[dest] = {}
                routeRequests[dest][#routeRequests[dest]+1] = {type = "R", host = origin}
                
                local nttl = data:byte(2)-1
                if nttl > 1 then
                    local sent = {}
                    --Bcast request
                    for _node, n in pairs(nodes) do
                        if _node ~= node then --We mustn't send it to origin node
                            for host in pairs(n.hosts)do
                                if not sent[host] then
                                    sent[host] = true
                                    resendRouteRequest(data, _node, host, nttl)
                                end
                            end
                        end
                    end
                end
                sent = nil
            else
                --we've already requested this addr so if we get the route
                --we'll respond
                routeRequests[dest][#routeRequests[dest]+1] = {type = "R", host = origin}
            end
        elseif data:sub(1,1) == "H" then --Host found
            local nttl = data:byte(2)-1
            local host = data:sub(3)
            
            if not routes[host] then
                routes[host] = {router = origin, node = node}
                processRouteRequests(host)
            end
        end
    end
    
    network.core.setCallback("send", send)
    network.core.setCallback("bind", bindAddr)
    
    ---------------
    --Network stats&info
    
    local function getInfo()
        local res = {}
        
        res.interfaces = {}
        for k, node in pairs(getNodes())do
            res.interfaces[k] = {selfAddr = node.selfAddr, linkName = node.linkName}
        end
        return res
    end
    
    local function getRoutingTable()
        local res = {}
        
        for k,v in pairs (routes) do
            if v.router then
                res[k] = {router = v.router, interface = v.node}
            end
        end
        return res
    end
    
    local function getArpTable(interface)
        local res = {}
        for k in pairs(nodes[interface].hosts)do
            table.insert(res, k)
        end
        return res
    end
    
    network.core.setCallback("netstat", getInfo)
    network.core.setCallback("intstat", getInterfaceInfo)
    network.core.setCallback("routetab", getRoutingTable)
    network.core.setCallback("arptab", getArpTable)
    
    network.core.lockCore()
end

event.listen("init", start)

