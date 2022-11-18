local network = require "network"
local event = require "event"
local term = require "term"

local args = {...}

local listen = false
local port = -1
local addr

for _,par in ipairs(args) do
    if par == "-l" then
        listen = true
    elseif port < 1 then
        local p = tonumber(par)
        if p then
            port = p
        end
    else
        addr = par
    end
end

if port < 0 then error("Unspecified port")end
if not listen and not addr then error("Unspecified address")end


local function handleTcp()
    local channel
    while true do
        local e = {event.pull()}
        if e[1] then
            if e[1] == "tcp" then
                if e[2] == "connection" then
                    channel = e[3]
                    if listen then network.tcp.unlisten(port)end
                    print("connected")
                elseif e[2] == "message" then
                    term.write(e[4])
                end
            elseif e[1] == "key_up" and channel then
                network.tcp.send(channel, string.char(e[3]))
                term.write(string.char(e[3]))
            end
        end
    end
end

if listen then
    network.tcp.listen(port)
    handleTcp()
else
    network.tcp.open(addr, port)
    handleTcp()
end
