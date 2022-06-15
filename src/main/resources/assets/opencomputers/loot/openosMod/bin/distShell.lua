local networks = require("networks")
local shell = require("shell")
local event = require("event")
local serialization = require("serialization")
local term = require("term")
local su = require("superUtiles")

------------------------------------

local appName = "distShell"
local args, options = shell.parse(...)

------------------------------------

if not _G.shells then _G.shells = {} end

------------------------------------

if args[1] == "open" then
    checkArg(1, args[3], "string")
    local network = networks.getNetwork(args[2])
    if not network then
        if not options.q then print("no this network") end
        return "no this network"
    end
    local function listen(_, networkName, lappName, index, side, command, ...)
        if networkName == network.name and lappName == appName and index == args[3] and side == "call" then 
            local code, err = load(command)
            if not code then network.send(lappName, index, "return", {nil, err}) return end
            network.send(lappName, index, "return", {pcall(code, ...)})
        end
    end
    event.listen("network_message", listen)
    table.insert(_G.shells, {network = network, index = args[3], listen = {"network_message", listen}})
elseif args[1] == "close" then
    local network = networks.getNetwork(args[2])
    if not network then
        if not options.q then print("no this network") end
        return "no this network"
    end
    local obj
    for i = 1, #_G.shells do
        local lobj = _G.shells[i]
        if lobj.index == args[3] then
            obj = lobj
            table.remove(_G.shells, i)
            break
        end
    end
    if not obj then
        if not options.q then print("no this shell") end
        return "no this shell"
    end
    event.ignore(table.unpack(obj.listen))
elseif args[1] == "send" then
    local network = networks.getNetwork(args[2])
    if not network then
        if not options.q then print("no this network") end
        return "no this network"
    end
    network.send(appName, args[3], "call", args[4], table.unpack(args, 5))
    local _, _, _, _, _, tbl = event.pull(4, "network_message", network.name, appName, args[3], "return")
    if tbl then
        if not options.q then print(serialization.serialize(tbl)) end
        return tbl
    else
        if not options.q then print("no result") end
    end
elseif args[1] == "shell" then
    local network = networks.getNetwork(args[2])
    if not network then
        if not options.q then print("no this network") end
        return "no this network"
    end
    local function send(command, ...)
        network.send(appName, args[3], "call", command, ...)
        local _, _, _, _, _, tbl = event.pull(4, "network_message", network.name, appName, args[3], "return")
        if tbl then
            return tbl
        end
    end
    print("LUA SHELL")
    print("network: ", args[2])
    print("device: ", args[3])
    while true do
        term.write("LUA: ")
        local input = io.read()
        if not input then return end
        print(serialization.serialize(send(input) or {}))
    end
elseif args[1] == "list" then
    for i = 1, #_G.shells do
        print(_G.shells[i].index)
    end
elseif args[1] == "sendFile" then
    local network = networks.getNetwork(args[2])
    if not network then
        if not options.q then print("no this network") end
        return "no this network"
    end
    network.send(appName, args[3], "call", assert(su.getFile(shell.resolve(args[4]))), table.unpack(args, 5))
    local _, _, _, _, _, tbl = event.pull(4, "network_message", network.name, appName, args[3], "return")
    if tbl then
        if not options.q then print(serialization.serialize(tbl)) end
        return tbl
    else
        if not options.q then print("no result") end
    end
else
    if not options.q then
        print("usege:")
        print("options [-q do not print]")
        print("distShell open network index")
        print("distShell close network index")
        print("distShell send network index command")
        print("distShell shell network index")
        print("distShell list")
        print("distShell sendFile network index path")
    end
    return "usege printed"
end