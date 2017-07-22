local network = require "network"

local function fillText(text, n)
    for k = 1, n - #text do
        text = text .. " "
    end
    return text
end

print("MCNET routing table")
local routes = network.info.getRoutes()
local maxlen = {12, 8, 5}

for host, route in pairs(routes) do
    maxlen[1] = maxlen[1] < #host+1 and #host+1 or maxlen[1]
    maxlen[2] = maxlen[2] < #route.router+1 and #route.router+1 or maxlen[2]
    maxlen[3] = maxlen[3] < #route.interface+1 and #route.interface+1 or maxlen[3]
end

print(fillText("Destination", maxlen[1])..fillText("Gateway", maxlen[2])..fillText("Iface", maxlen[3]))

for host, route in pairs(routes) do
    print(fillText(host, maxlen[1])..fillText(route.router, maxlen[2])..fillText(route.interface, maxlen[3]))
end

