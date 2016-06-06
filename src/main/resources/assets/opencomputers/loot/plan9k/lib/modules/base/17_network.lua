--local network = require "network"

local cfg = {
    routereq_retry = 20, --Seconds betwen route seek retries
    routereq_drop = 115, --Seconds to stop seeking route
    routereq_relayage = 3, --Timeout added to each request re-sent
    route_relayage = 3, --Age added to each route sent
    route_exp = 300/2, --Remove route after being not used for this time
    route_recheck = 320/2, --Recheck each route after this amount of time
    route_age_max = 660, -- Max age of route
    ttl = 32 --Time to live
}
kernel.modules.procfs.data.sys.net = cfg

local _rawSend
local isAccessible
local getNodes
--local getInterfaceInfo
local startNetwork
local resetRouting

local dataHandler --Layer 2 data handler

accessibleHosts = {}
nodes = {}

------------------------
--Layer 1

local initated = false

function start()
    if initated then return end
    initated = true
    
    --local filesystem = require "filesystem"

    local drivers = {}

    for file in kernel.modules.vfs.list("/lib/modules/network") do
        
        --print("Loading driver:", file)
        local ld, reason = kernel.userspace.loadfile("/lib/modules/network/"..file, nil, _G)
        if not ld then
            kernel.io.println("Network driver loading failed: " .. tostring(reason))
        end
        drivers[file] = {driver = ld()}
        
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
        
        function eventHandler.delInterface(interface)
            nodes[interface] = nil
            for addr, host in pairs(accessibleHosts) do
                if host.node == interface then
                    accessibleHosts[addr] = nil
                end
            end
            resetRouting()
        end
        
        function eventHandler.recvData(data, node, origin)
            dataHandler(data, node, origin)
        end
        
        function eventHandler.setListener(evt, listener)
            return kernel.modules.keventd.listen(evt, function(...)
                local args = {...}
                local res = {pcall(function()listener(table.unpack(args))end)}
                if not res[1] then
                    kernel.io.println("ERROR IN NET EVENTHANDLER["..file.."]:"..tostring(res[2]))
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
    
    kernel.io.println("Link Control initated")
    startNetwork()
    kernel.io.println("Network initated")
end

------------------------
--Layer 2

startNetwork  = function()
    local rawSend
    --local send

    local routeRequests = {} -- Table by dest addressed of tables {type = T[, data=..]}, types: D(own waiting data), R(route request for someone), E(routed data we should be able to route..)
    local routes = {} --Table of pairs -> [this or route] / {thisHost=true} / {router = [addr]}
    
    resetRouting = function()
        for k, route in pairs(routes) do
            if not route.thisHost then
                routes[k] = nil
            end
        end
    end
    
    routes[computer.address()] = {thisHost=true}
    
    -----Data out
    
    local function onRecv(origin, data)
        --computer.pushSignal("network_message", origin, data)
        kernel.modules.libnetwork.handleData(origin, data)
    end
    
    -----Sending
    
    local function sendDirectData(addr, data)--D[ttl-byte][data]
        return rawSend(addr, string.pack("c1B", "D", cfg.ttl)..data)
    end
    
    local function sendRoutedData(addr, data)--E[ttl-byte][hostlen-byte][dest host][hostlen-byte][origin host]message
        local nodes = getNodes()
        local msg = string.pack("c1Bs1s1", "E", cfg.ttl, addr, nodes[routes[addr].node].selfAddr) .. data
        local node, driver = isAccessible(addr)
        _rawSend(node and addr or routes[addr].router, node or routes[addr].node, msg)
    end
    
    local function sendRoutedDataAs(addr, origin, data, ottl)--E[ttl-byte][hostlen-byte][dest host][hostlen-byte][origin host]message
        local msg = string.pack("c1Bs1s1", "E", ottl - 1, addr, origin) .. data
        local node, driver = isAccessible(addr)
        _rawSend(node and addr or routes[addr].router, node or routes[addr].node, msg)
    end
    
    local function sendRouteRequest(addr, age)--R[ttl-byte][Addr len][Requested addr][age]
        local request = string.pack(">c1Bs1H", "R", cfg.ttl, addr, math.floor(age))
        local nodes = getNodes()
        local sent = {}
        for node, n in pairs(nodes) do
            for host in pairs(n.hosts)do
                if not sent[host]then
                    sent[host] = true
                    --_rawSend(host, node, base..toByte(n.selfAddr:len())..n.selfAddr)
                    _rawSend(host, node, request)
                end
            end
        end
        sent = nil
    end
    
    local function sendHostFound(dest, age, addr, dist)--H[ttl-byte][age-short][distance][Found host]
        --kernel.io.println("found "..addr.." for "..dest)
        if dest ~= "localhost" then
            return rawSend(dest, "H"..string.pack(">BHB", cfg.ttl, math.floor(age + cfg.route_relayage), dist)..addr)
        end
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
                    routes[addr].active = computer.uptime()
                end
            else
                --route is unknown, we have to request it if we haven't done so already
                if not routeRequests[addr] then 
                    routeRequests[addr] = {update = computer.uptime(), timeout = computer.uptime()}
                    routeRequests[addr][#routeRequests[addr]+1] = {type = "D", data = data}
                    sendRouteRequest(addr, 0)
                else
                    routeRequests[addr].timeout = computer.uptime()
                    routeRequests[addr][#routeRequests[addr]+1] = {type = "D", data = data}
                end
            end
        end
    end
    
    local function processRouteRequests(host, age, dist)
        if routeRequests[host] then
            age = age or (computer.uptime() - routeRequests[host].timeout)
            for t, request in pairs(routeRequests[host]) do
                if type(request) == "table" then
                    if request.type == "D" then
                        sendRoutedData(host, request.data)
                    elseif request.type == "E" then
                        if request.ttl-1 > 1 then
                            sendRoutedDataAs(host, request.origin, request.data, request.ttl)
                        end
                    elseif request.type == "R" then
                        sendHostFound(request.host, age, host, dist or 1)
                    end
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
        routes[addr] = {thisHost=true, dist = 0}
        processRouteRequests(addr, 0, 1)
    end
    
    kernel.modules.keventd.listen("hostname", function(_, name) bindAddr(name)end)
    
    dataHandler = function(data, node, origin)
        if data:sub(1,1) == "D" then --Direct data
            onRecv(origin, data:sub(3))
        elseif data:sub(1,1) == "E" then --Routed data
            --local ttl = data:byte(2)
            --local dest, destlen = readSizeStr(data, 3)
            --local orig, origlen = readSizeStr(data, 3+destlen)
            local ttl, dest, orig, dstart = string.unpack(">Bs1s1", data, 2)
            local dat = data:sub(dstart)
            
            
            if checkRouteDest(dest, orig, node, dat) then
                onRecv(orig, dat)
            else
                local _node, driver = isAccessible(dest)
                if _node then --Have direct route
                    if ttl-1 > 0 then
                        kernel.io.println("Direct hop orig="..orig.." -> dest="..dest)
                        sendRoutedDataAs(dest, orig, dat, ttl)
                    end
                else
                    if routes[dest] then --Have route
                        if ttl-1 > 0 then
                            sendRoutedDataAs(dest, orig, dat, ttl)
                            routes[dest].active = computer.uptime()
                        end
                    else --Need to find route
                        if not routeRequests[dest] then
                            routeRequests[dest] = {update = computer.uptime(), timeout = computer.uptime()}
                        end
                        routeRequests[dest].timeout = computer.uptime()
                        if not routeRequests[dest] then routeRequests[dest] = {update = computer.uptime(), timeout = computer.uptime()} end
                        routeRequests[dest][#routeRequests[dest]+1] = {type = "E", origin = orig, ttl = ttl, data = dat}
                        sendRouteRequest(dest, 0)
                    end
                end
            end
        elseif data:sub(1,1) == "R" then --Route request
            --local dest, l = readSizeStr(data, 3)
            local nttl, dest, age = string.unpack(">Bs1H", data, 2)
            if age > cfg.routereq_drop then
                return
            end
            if not routeRequests[dest] then
                
                --check if accessible interface
                local nodes = getNodes()
                for _node, n in pairs(nodes) do
                    if _node ~= node then --requested host won't ever be in same node
                        for host in pairs(n.hosts)do
                            if host == dest then
                                --Found it!
                                sendHostFound(origin, 0, dest, 1)
                                return
                            end
                        end
                    end
                end
                
                --check if route known
                if routes[dest] then
                    if routes[dest].thisHost then
                        --sendHostFound(origin, nodes[node].selfAddr)
                        sendHostFound(origin, 0 , dest, 1)
                    elseif routes[dest].router ~= origin then--Router might have rebooted and is asking about route
                        sendHostFound(origin, computer.uptime() - routes[dest].age, dest, routes[dest].dist + 1)
                        --routes[dest].active = computer.uptime()
                    end
                    return
                end
                
                if isAccessible(dest) then
                    kernel.io.println("Attempt to seek route to direct host(0x1WAT)")
                    kernel.io.println("seeker " .. origin .. " to " .. dest)
                    return
                end
                
                routeRequests[dest] = {update = computer.uptime(), timeout = computer.uptime() - age}
                routeRequests[dest][#routeRequests[dest]+1] = {type = "R", host = origin}
                
                --local nttl = data:byte(2)-1
                if nttl > 1 then
                    local sent = {}
                    --Bcast request
                    for _node, n in pairs(nodes) do
                        if _node ~= node then --We mustn't send it to origin node
                            for host in pairs(n.hosts) do
                                if not sent[host] then
                                    sent[host] = true
                                    --resend route request
                                    local msg = string.pack(">c1Bs1H", "R", nttl - 1, dest, age + cfg.routereq_relayage)
                                    _rawSend(host, _node, msg)
                                end
                            end
                        end
                    end
                end
                sent = nil
            else
                if isAccessible(dest) then
                    kernel.io.println("Attempt to seek route to direct host(0x2WAT)")
                    kernel.io.println("seeker " .. origin .. " to " .. dest)
                    return
                end
                --we've already requested this addr so if we get the route
                --we'll respond. TODO: Duplicates?
                if computer.uptime() - routeRequests[dest].timeout > age then
                    routeRequests[dest].timeout = computer.uptime() - age
                end
                routeRequests[dest][#routeRequests[dest]+1] = {type = "R", host = origin}
            end
        elseif data:sub(1,1) == "H" then --Host found
            local nttl, age, dist, n = string.unpack(">BHB", data, 2)
            local host = data:sub(n)
            
            if not isAccessible(host) then
                if not routes[host] then
                    routes[host] = {
                        router = origin,
                        node = node,
                        age = computer.uptime() - age,
                        active = computer.uptime(),
                        dist = dist
                    }
                    processRouteRequests(host, age, dist + 1)
                else
                    if (routes[host].dist > dist) or
                       ((routes[host].age < computer.uptime() - age) and (routes[host].dist >= dist)) then
                        routes[host] = {
                            router = origin,
                            node = node,
                            age = computer.uptime() - age,
                            active = routes[host].active,
                            dist = dist
                        }
                    end
                end
            end
        end
    end
    
    --network.core.setCallback("send", send)
    --network.core.setCallback("bind", bindAddr)
    
    ---------------
    --Network stats&info
    
    function getInfo()
        local res = {}
        
        res.interfaces = {}
        for k, node in pairs(getNodes())do
            res.interfaces[k] = {selfAddr = node.selfAddr, linkName = node.linkName}
        end
        return res
    end
    
    function getRoutingTable()
        local res = {}
        local now = computer.uptime()
        
        for k,v in pairs(routeRequests) do
            res[k] = {router = "", interface = "", age = now - v.timeout, dist = 0}
        end
        
        for k,v in pairs(routes) do
            if v.router then
                res[k] = {router = v.router, interface = v.node, age = now - v.age, dist = v.dist}
            elseif v.thisHost then
                res[k] = {router = computer.address(), interface = "lo", age = 0, dist = 0}
            end
        end
        return res
    end
    
    function getArpTable(interface)
        local res = {}
        for k in pairs(nodes[interface].hosts)do
            table.insert(res, k)
        end
        return res
    end
    
    --network.core.setCallback("netstat", getInfo)
    --network.core.setCallback("intstat", getInterfaceInfo)
    --network.core.setCallback("routetab", getRoutingTable)
    --network.core.setCallback("arptab", getArpTable)
    
    --network.core.lockCore()
    
    kernel.modules.timer.add(function()
        local now = computer.uptime()
        --Route request timeouts, re-requests
        for host, request in pairs(routeRequests) do
            if now - request.update >= cfg.routereq_retry then
                if routes[host] then
                    processRouteRequests(host, now - routes[host].age, routes[host].dist + 1)
                else
                    sendRouteRequest(host, now - request.update)
                    request.update = computer.uptime()
                end
            end
            if now - request.timeout >= cfg.routereq_drop then
                routeRequests[host] = nil
            end
        end
        
        --Route timeouts, rechecks, 
        for host, route in pairs(routes) do
            if not route.thisHost then
                local age = now - route.age
                if age >= cfg.route_recheck then
                    sendRouteRequest(host, 0)
                end
                if age >= cfg.route_age_max then
                    routes[host] = nil
                else
                    if now - route.active >= cfg.route_exp then
                        routes[host] = nil
                    end
                end
            end
        end
    end, 5)
end

