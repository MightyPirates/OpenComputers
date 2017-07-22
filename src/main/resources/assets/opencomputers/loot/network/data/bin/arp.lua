local network = require "network"

local function fillText(text, n)
    for k = 1, n - #text do
        text = text .. " "
    end
    return text
end

local maxlen = {8, 5}

for interface in pairs(network.info.getInfo().interfaces) do
    maxlen[2] = maxlen[2] < #interface+1 and #interface+1 or maxlen[2]
    for _, host in ipairs(network.info.getArpTable(interface)) do
        maxlen[1] = maxlen[1] < #host+1 and #host+1 or maxlen[1]
    end
end

print(fillText("Address", maxlen[1])..fillText("Iface", maxlen[2]))

for interface in pairs(network.info.getInfo().interfaces) do
    for _, host in ipairs(network.info.getArpTable(interface)) do
        print(fillText(host, maxlen[1])..fillText(interface, maxlen[2]))
    end
end

