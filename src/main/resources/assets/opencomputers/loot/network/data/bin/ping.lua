local network = require "network"
local event = require "event"
local computer = require "computer"
local shell = require "shell"

local args, options = shell.parse(...)

if #args < 1 or options.h or options.help then
    print("Usage: ping: [addr]")
    print("  --c= --count=[ping count]  Amount of pings to send(default 6)")
    print("  --s= --size=[data size]    Payload size(default 56 bytes)")
    print("  --i= --interval=[seconds]  Ping interval(default 1s)")
    --print("  -d   --duplicates          Check for duplicate messages")
    print("  --t= --droptime=[seconds]  Amount of time after which ping is")
    print("                             Considered to be lost[default 8s]")
    print("  -v   --verbose             Output more details")
    return
end

local len = tonumber(options.s) or tonumber(options.size) or 56

local function round(n,r) return math.floor(n*(10^r))/(10^r) end

local function verbose(...)
    if options.v or options.verbose then
        print(...)
    end
end

local function generatePayload()
    local payload = ""
    for i = 1, len do
        local ch = string.char(math.random(0, 255))
        ch = ch == ":" and "_" or ch --Ping matcher derps hard when it finds ':' in payload
        payload = payload .. ch
    end
    return payload
end


print("PING "..args[1].." with "..tostring(len) .." bytes of data")

local stats = {
    transmitted = 0,
    received = 0,
    malformed = 0
}

local function doSleep()
    
    local deadline = computer.uptime() + (tonumber(options.i) or tonumber(options.interval) or 1)
    repeat
        event.pull(deadline - computer.uptime())
    until computer.uptime() >= deadline
end

local function doPing()
    
    local payload = generatePayload()
    local icmp_seq = network.icmp.ping(args[1], payload)
    stats.transmitted = stats.transmitted + 1
    verbose(tostring(len).." bytes to "..args[1]..": icmp_seq="..tostring(icmp_seq))
    local start = computer.uptime()
    
    local deadline = start + (tonumber(options.t) or tonumber(options.droptime) or 8)
    local e, replier, id, inpayload
    repeat
        e, replier, id, inpayload = event.pull(deadline - computer.uptime(), "ping_reply")
    until computer.uptime() >= deadline or (e == "ping_reply" and id == icmp_seq)
    
    if computer.uptime() >= deadline and e ~= "ping_reply" then
        verbose(tostring(len).." bytes lost: icmp_seq="..tostring(icmp_seq))
    elseif inpayload == payload then
        stats.received = stats.received + 1
        print(tostring(len).." bytes from "..args[1]..": icmp_seq="..tostring(icmp_seq).." time="..tostring(round(computer.uptime()-start,2)).." s")
    else
        stats.malformed = stats.malformed + 1
        verbose(tostring(#inpayload).." bytes malformed: icmp_seq="..tostring(icmp_seq).." time="..tostring(round(computer.uptime()-start,2)).." s")
    end
end

local begin = computer.uptime()

local function outputStats()
    print("--- "..args[1].." ping statistics ---")
    print(tostring(stats.transmitted) .. " packets transmitted, "
        ..tostring(stats.received) .. " received, "
        ..tostring(100 - math.floor((stats.received / stats.transmitted) * 100)) .. "% packet loss, time " .. tostring(round(computer.uptime()-begin,2)))
end

local state, reason = pcall(function()
    local c = 0
    repeat
        doPing()
        doSleep()
        c = c + 1
    until c == 0 or (tonumber(options.c) and c >= tonumber(options.c)) 
                 or (tonumber(options.count) and c >= tonumber(options.count)) 
                 or ((not tonumber(options.c)) and (not tonumber(options.count)) and c >= 8)
end)

if not state then
    verbose("Stopped by: "..tostring(reason))
end

outputStats()

