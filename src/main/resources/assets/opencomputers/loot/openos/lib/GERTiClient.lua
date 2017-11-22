-- GERT v1.0 - Release
local GERTi = {}
local component = require("component")
local computer = require("computer")
local event = require("event")
local serialize = require("serialization")
local modem = nil
local tunnel = nil

if (not component.isAvailable("tunnel")) and (not component.isAvailable("modem")) then
	io.stderr:write("This program requires a network or linked card to run.")
	os.exit(1)
end

if (component.isAvailable("modem")) then
	modem = component.modem
	modem.open(4378)

	if (component.modem.isWireless()) then
		modem.setStrength(500)
	end
end

if (component.isAvailable("tunnel")) then
	tunnel = component.tunnel
end

-- addresses
local iAddress = nil
local cachedAddress = {{}, {}, {}, {}, {}}
local addressDex = 1

-- Tables of neighbors and connections
-- neighbors[x]{"address", "port", "tier"}
local neighbors = {}
local tier = 3
local neighborDex = 1

-- connections[x]{"destination", "origination", "data", "dataDex", "connectionID", "doEvent"} Connections are established at endpoints
local connections = {}
local connectDex = 1
-- paths[x]{"destination", "origination",  "nextHop", "port"}
local paths = {}
local pathDex = 1

local handler = {}

local function sortTable(elementOne, elementTwo)
	return (tonumber(elementOne["tier"]) < tonumber(elementTwo["tier"]))
end

-- this function adds a handler for a set time in seconds, or until that handler returns a truthful value (whichever comes first)
local function addTempHandler(timeout, code, cb, cbf)
	local disable = false
	local function cbi(...)
		if disable then return end
		local evn, rc, sd, pt, dt, code2 = ...
		if code ~= code2 then return end
		if cb(...) then
			disable = true
			return false
		end
	end
	event.listen("modem_message", cbi)
	event.timer(timeout, function ()
		event.ignore("modem_message", cbi)
		if disable then return end
		cbf()
	end)
end
-- Like a sleep, but it will exit early if a modem_message is received and then something happens.
local function waitWithCancel(timeout, cancelCheck)
	-- Wait for the response.
	local now = computer.uptime()
	local deadline = now + 5
	while now < deadline do
		event.pull(deadline - now, "modem_message")
		-- The listeners were called, so as far as we're concerned anything cancel-worthy should have happened
		local response = cancelCheck()
		if response then return response end
		now = computer.uptime()
	end
	-- Out of time
	return cancelCheck()
end

local function storeNeighbors(sendingModem, port, package)
	-- Register neighbors for communication to the rest of the network
	neighbors[neighborDex] = {}
	neighbors[neighborDex]["address"] = sendingModem
	neighbors[neighborDex]["port"] = tonumber(port)
	if package == nil then
		--This is used for when a computer receives a new client's AddNeighbor message. It stores a neighbor connection with a tier one lower than this computer's tier
		neighbors[neighborDex]["tier"] = (tier+1)
	else
		-- This is used for when a computer receives replies to its AddNeighbor message.
		neighbors[neighborDex]["tier"] = tonumber(package)
		if tonumber(package) < tier then
			-- attempt to set this computer's tier to one lower than the highest ranked neighbor.
			tier = tonumber(package)+1
		end
	end
	neighborDex = neighborDex + 1
	-- sort table so that the best connection to the Master Network Controller (MNC) comes first
	table.sort(neighbors, sortTable)

	return true
end

local function removeNeighbor(address)
	for key, value in pairs(neighbors) do
		if value["address"] == address then
			table.remove(neighbors, key)
			break
		end
	end
end

local function storeConnection(origination, destination, doEvent, connectionID, originGAddress)
	connections[connectDex] = {}
	connections[connectDex]["destination"] = destination
	connections[connectDex]["origination"] = origination
	connections[connectDex]["originationGAddress"] = originGAddress
	connections[connectDex]["data"] = {}
	connections[connectDex]["dataDex"] = 1
	connections[connectDex]["connectionID"] = (connectionID or connectDex)
	connections[connectDex]["doEvent"] = (doEvent or false)
	connectDex = connectDex + 1
	return connectionID or (connectDex-1)
end
local function storePath(origination, destination, nextHop, port)
	paths[pathDex] = {}
	paths[pathDex]["origination"] = origination
	paths[pathDex]["destination"] = destination
	paths[pathDex]["nextHop"] = nextHop
	paths[pathDex]["port"] = port
	pathDex = pathDex + 1
	return (pathDex-1)
end
-- Stores data inside a connection for use by a program
local function storeData(connectionID, data, origination)
	local connectNum
	for key, value in pairs(connections) do
		if value["connectionID"] == connectionID and value["origination"] == origination then
			connectNum = key
			break
		end
	end
	local dataNum = connections[connectNum]["dataDex"]

	if dataNum >= 20 then
		table.remove(connections[connectNum]["data"], 1)
	end

	connections[connectNum]["data"][dataNum]=data
	connections[connectNum]["dataDex"] = math.min(dataNum + 1, 20)
	if connections[connectNum]["doEvent"] then
		computer.pushSignal("GERTData", connections[connectNum]["originationGAddress"], connections[connectNum]["origination"], connectionID)
	end
	return true
end

local function cacheAddress(gAddress, realAddress)
	if addressDex > 5 then
		addressDex = 1
		return cacheAddress(gAddress, realAddress)
	end
	cachedAddress[addressDex]["gAddress"] = gAddress
	cachedAddress[addressDex]["realAddress"] = realAddress
end

-- Low level function that abstracts away the differences between a wired/wireless network card and linked card.
local function transmitInformation(sendTo, port, ...)
	if (port ~= 0) and (modem) then
		return modem.send(sendTo, port, ...)
	elseif (tunnel) then
		return tunnel.send(...)
	end

	io.stderr:write("Tried to transmit, but no network card or linked card was found.")
	return false
end

local function resolveAddress(gAddress)
	for key, value in pairs(cachedAddress) do
		if value["gAddress"] == gAddress then
			return value["realAddress"]
		end
	end
	local response
	transmitInformation(neighbors[1]["address"], neighbors[1]["port"], "ResolveAddress", gAddress)
	addTempHandler(3, "ResolveComplete", function (_, _, _, _, _, code, returnAddress)
			response = returnAddress
		end, function () end)
	waitWithCancel(3, function () return response end)
	return response
end

handler["AddNeighbor"] = function (sendingModem, port, code)
	-- Process AddNeighbor messages and add them as neighbors
	if tier < 3 then
		storeNeighbors(sendingModem, port, nil)
		return transmitInformation(sendingModem, port, "RETURNSTART", tier)
	end
	return false
end

handler["CloseConnection"] = function(sendingModem, port, code, connectionID, destination, origin)
	for key, value in pairs(paths) do
		if value["destination"] == destination and value["origination"] == origin then
			if value["nextHop"] ~= (modem or tunnel).address then
				transmitInformation(value["nextHop"], value["port"], "CloseConnection", connectionID, destination, origin)
			end
			table.remove(paths, key)
			break
		end
	end
	for key, value in pairs(connections) do
		if value["connectionID"] == connectionID and value["origination"] == origin then
			table.remove(connections, key)
			break
		end
	end
end

handler["DATA"] = function (sendingModem, port, code, data, destination, origination, connectionID)
	-- Attempt to determine if host is the destination, else send it on to next hop.
	for key, value in pairs(paths) do
		if value["destination"] == destination and value["origination"] == origination then
			if value["destination"] == (modem or tunnel).address then
				return storeData(connectionID, data, origination)
			else
				return transmitInformation(value["nextHop"], value["port"], "DATA", data, destination, origination, connectionID)
			end
		end
	end
	return false
end

-- opens a route using the given information, used in handler["OPENROUTE"] and GERTi.openSocket
local function routeOpener(destination, origination, beforeHop, nextHop, receivedPort, transmitPort, outbound, connectionID, originGAddress)
	local function sendOKResponse(isDestination)
		transmitInformation(beforeHop, receivedPort, "ROUTE OPEN", destination, origination)
		if isDestination then
			storePath(origination, destination, nextHop, transmitPort)
			local newID = storeConnection(origination, destination, false, connectionID, originGAddress)
			return computer.pushSignal("GERTConnectionID", originGAddress, newID)
		else
			return storePath(origination, destination, nextHop, transmitPort)
		end
	end
	if (modem or tunnel).address ~= destination then
		local connect1 = 0
		transmitInformation(nextHop, transmitPort, "OPENROUTE", destination, nextHop, origination, outbound, connectionID, originGAddress)
		addTempHandler(3, "ROUTE OPEN", function (eventName, recv, sender, port, distance, code, pktDest, pktOrig)
			if (destination == pktDest) and (origination == pktOrig) then
				connect1 = sendOKResponse(false)
				return true -- This terminates the wait
			end
		end, function () end)
		waitWithCancel(3, function () return response end)
		return connect1
	end
	return sendOKResponse(true)
end

handler["OPENROUTE"] = function (sendingModem, port, code, destination, intermediary, origination, outbound, connectionID, originGAddress)
	-- Attempt to determine if the intended destination is this computer
	if destination == modem.address then
		return routeOpener(modem.address, origination, sendingModem, modem.address, port, port, outbound, connectionID, originGAddress)
	end

	-- attempt to check if destination is a neighbor to this computer, if so, re-transmit OPENROUTE message to the neighbor so routing can be completed
	for key, value in pairs(neighbors) do
		if value["address"] == destination then
			return routeOpener(destination, origination, sendingModem, neighbors[key]["address"], port, neighbors[key]["port"], outbound, connectionID, originGAddress)
		end
	end

	-- if it is not a neighbor, and no intermediary was found, then contact parent to forward indirect connection request
	if intermediary == modem.address then
		return routeOpener(destination, origination, sendingModem, neighbors[1]["address"], port, neighbors[1]["port"], outbound, connectionID, originGAddress)
	end

	-- If an intermediary is found (likely because MNC was already contacted), then attempt to forward request to intermediary
	for key, value in pairs(neighbors) do
		if value["address"] == intermediary then
			return routeOpener(destination, origination, sendingModem, intermediary, port, neighbors[key]["port"], outbound, connectionID, originGAddress)
		end
	end
end

handler["RemoveNeighbor"] = function (sendingModem, port, code, origination)
	removeNeighbor(origination)
	transmitInformation(neighbors[1]["address"], neighbors[1]["port"], "RemoveNeighbor", origination)
end

handler["RegisterNode"] = function (sendingModem, sendingPort, code, origination, tier, serialTable)
	transmitInformation(neighbors[1]["address"], neighbors[1]["port"], "RegisterNode", origination, tier, serialTable)
	addTempHandler(3, "RegisterComplete", function (eventName, recv, sender, port, distance, code, targetMA, iResponse)
		if targetMA == origination then
			transmitInformation(sendingModem, sendingPort, "RegisterComplete", targetMA, iResponse)
			return true
		end
	end, function () end)
end

handler["ResolveAddress"] = function (sendingModem, port, code, gAddress)
	transmitInformation(neighbors[1]["address"], neighbors[1]["port"], "ResolveAddress", gAddress)
	addTempHandler(3, "ResolveComplete", function(_, _, sender, _, _, code, realAddress)
		transmitInformation(sendingModem, port, "ResolveComplete", realAddress)
		end, function() end)
end

handler["RETURNSTART"] = function (sendingModem, port, code, tier)
	-- Store neighbor based on the returning tier
	storeNeighbors(sendingModem, port, tier)
end

local function receivePacket(eventName, receivingModem, sendingModem, port, distance, code, ...)
	-- Attempt to call a handler function to further process the packet
	if handler[code] ~= nil then
		handler[code](sendingModem, port, code, ...)
	end
end

-- Begin startup ---------------------------------------------------------------------------------------------------------------------------
-- transmit broadcast to check for neighboring GERTi enabled computers
if tunnel then
	tunnel.send("AddNeighbor")
end
if modem then
	modem.broadcast(4378, "AddNeighbor")
end

-- Register event listener to receive packets from now on
event.listen("modem_message", receivePacket)

-- Wait a while to build the neighbor table.
os.sleep(2)

-- forward neighbor table up the line
local serialTable = serialize.serialize(neighbors)
local mncUnavailable = true
if serialTable ~= "{}" then
	-- Even if there is no neighbor table, still register to try and form a network regardless
	local addr = (modem or tunnel).address
	transmitInformation(neighbors[1]["address"], neighbors[1]["port"], "RegisterNode", addr, tier, serialTable)
	addTempHandler(3, "RegisterComplete", function (_, _, _, _, _, code, targetMA, iResponse)
		if targetMA == addr then
			iAddress = iResponse
			return true
		end
	end, function () end)
	if waitWithCancel(5, function () return iAddress end) then
		mncUnavailable = false
	end
end
if mncUnavailable then
	print("Unable to contact the MNC. Functionality will be impaired.")
end

-- Override computer.shutdown to allow for better network leaves
local function safedown()
	if tunnel then
		tunnel.send("RemoveNeighbor", tunnel.address)
	end
	if modem then
		modem.broadcast(4378, "RemoveNeighbor", modem.address)
	end
	for key, value in pairs(connections) do
		handler["CloseConnection"]((modem or tunnel).address, 4378, "CloseConnection", value["connectionID"], value["destination"], value["origination"])
	end
end
event.listen("shutdown", safedown)

-- startup procedure is now complete ------------------------------------------------------------------------------------------------------------
-- begin procedure to allow for data transmission

-- Writes data to an opened connection
local function writeData(self, data)
	return transmitInformation(self.nextHop, self.outPort, "DATA", data, self.destination, self.origination, self.ID)
end

-- Reads data from an opened connection
local function readData(self)
	if self.incDex then
		local data = connections[self.incDex]["data"]
		connections[self.incDex]["data"] = {}
		connections[self.incDex]["dataDex"] = 1
		return data
	else
		for key, value in pairs(connections) do
			if value["destination"] == self.origination and value["connectionID"] == self.ID and value["origination"] == self.destination then
				self.incDex = key
				if self.doEvent then
					value["doEvent"] = true
				end
				return self:read()
			end
		end
		return {}
	end
end

local function closeConnection(self)
	transmitInformation(self.nextHop, self.outPort, "CloseConnection", self.ID, self.destination, self.origination)
	handler["CloseConnection"]((modem or tunnel).address, 4378, "CloseConnection", self.ID, self.destination, self.origination)
end
-- This is the function that allows end-users to open sockets, which are the primary method of reading and writing data with GERT.
function GERTi.openSocket(gAddress, doEvent, provID)
	local destination, err = resolveAddress(gAddress)
	local origination = (modem or tunnel).address
	local nextHop
	local outID = (provID or connectDex)
	local outDex = 0
	local incDex = nil
	local outgoingPort = 0
	local isValid = false
	local socket = {}
	if not destination then
		return nil, err
	end
	if modem then
		outgoingPort = 4378
	end
	for key, value in pairs(neighbors) do
		if value["address"] == destination then
			outDex = storeConnection(origination, destination, false, provID, iAddress)
			nextHop = value["address"]
			routeOpener(destination, origination, origination, value["address"], value["port"], value["port"], gAddress, outID, iAddress)
			isValid = true
			break
		end
	end
	if not isValid then
		outDex = storeConnection(origination, destination, false, provID, iAddress)
		nextHop = neighbors[1]["address"]
		routeOpener(destination, origination, origination, neighbors[1]["address"], neighbors[1]["port"], neighbors[1]["port"], gAddress, outID, iAddress)
		isValid = true
	end
			
	if isValid then
		socket.origination = origination
		socket.destination = destination
		socket.outbound = gAddress
		socket.outPort = outgoingPort
		socket.nextHop = nextHop
		socket.ID = outID
		socket.incDex = incDex
		socket.outDex = outDex
		socket.write = writeData
		socket.read = readData
		socket.close = closeConnection
		socket.doEvent = doEvent
	else
		return nil, "Route cannot be opened, please confirm destination and that a valid path exists."
	end
	return socket
end

function GERTi.getConnections()
	local tempTable = {}
	for key, value in pairs(connections) do
		tempTable[key] = {}
		tempTable[key]["destination"] = value["destination"]
		tempTable[key]["origination"] = value["origination"]
		tempTable[key]["connectionID"] = value["connectionID"]
		tempTable[key]["doEvent"] = value["doEvent"]
	end
	return tempTable
end

function GERTi.getNeighbors()
	return neighbors
end

function GERTi.getPaths()
	return paths
end

function GERTi.getAddress()
	return iAddress
end
return GERTi