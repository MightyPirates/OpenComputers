local network = require "network"
local event = require "event"
local computer = require "computer"

local args = {...}

if #args < 1 then
    print("Usage: ping: [addr]")
end

print("Pinging "..args[1].." with 16 bytes of data:")
local pingID = network.icmp.ping(args[1], "0123456789abcdef")
local start = computer.uptime()

local e, replier, id, payload
repeat
e, replier, id, payload = event.pull("ping_reply")
until id == pingID

print("Got reply in "..tostring(computer.uptime()-start).." seconds")

