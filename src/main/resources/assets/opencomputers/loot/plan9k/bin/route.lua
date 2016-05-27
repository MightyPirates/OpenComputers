local network = require "network"

local function fillText(text, n)
    for k = 1, n - #text do
        text = text .. " "
    end
    return text
end

local function normLine(data)
    local res = ""
    for c in data:gmatch(".") do
        if c == "\n" or c == "\r" then c = "\x1b[31m.\x1b[39m" end
        res = res .. (c:match("[%g%s]") or "\x1b[31m.\x1b[39m")
    end
    return res
end

print("MCNET routing table")
local routes = network.info.getRoutes()
local maxlen = {12, 8, 6, 4, 5}

for host, route in pairs(routes) do
    maxlen[1] = maxlen[1] < #normLine(host)+1 and #normLine(host)+1 or maxlen[1]
    maxlen[2] = maxlen[2] < #route.router+1 and #route.router+1 or maxlen[2]
    maxlen[3] = maxlen[3] < #route.interface+1 and #route.interface+1 or maxlen[3]
    maxlen[4] = maxlen[4] < #tostring(route.age)+1 and #tostring(route.age)+1 or maxlen[4]
    maxlen[5] = maxlen[5] < #tostring(route.dist)+1 and #tostring(route.dist)+1 or maxlen[5]
end

print(fillText("Destination", maxlen[1])..
      fillText("Gateway", maxlen[2])..
      fillText("Iface", maxlen[3])..
      fillText("Age", maxlen[4])..
      fillText("Dist", maxlen[5]))

for host, route in pairs(routes) do
    print(fillText(normLine(host), maxlen[1])..
          fillText(route.router, maxlen[2])..
          fillText(route.interface, maxlen[3])..
          fillText(tostring(route.age), maxlen[4])..
          fillText(tostring(route.dist), maxlen[5]))
end

