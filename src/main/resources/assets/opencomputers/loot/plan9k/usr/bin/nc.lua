local network = require "network"
local event = require "event"

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

local chanel
local function handleTcp()
    while true do
        while io.input().remaining() ~= 0 do
            local data = io.read(math.min(io.input().remaining(), 7000))
            if not data then
                
            end
            network.tcp.send(chanel, data)
        end
        local e = {event.pull()}
        if e[1] then
            if e[1] == "tcp" then
                if e[2] == "connection" then
                    if listen and e[5] == port and e[6] == "incoming" then
                        network.tcp.unlisten(port)
                        print("connected")
                    elseif not listen and e[3] == chanel and e[6] ~= "incoming" then
                        chanel = e[3]
                        print("connected")
                    end
                elseif e[2] == "close" and e[3] == chanel then
                    print("Connection closed")
                    return
                elseif e[2] == "message" and e[3] == chanel then
                    io.write(e[4])
                end
            end
        end
    end
end

if listen then
    network.tcp.listen(port)
    handleTcp()
else
    chanel = network.tcp.open(addr, port)
    handleTcp()
end
