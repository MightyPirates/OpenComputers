local event = require "event"
local component = require "component"
local keyboard = require "keyboard"
local serialization = require "serialization"

local args = {...}
local color, isPal, evt

local function normLine(data)
    local res = ""
    for c in data:gmatch(".") do
        if c == "\n" or c == "\r" then c = "\x1b[31m.\x1b[39m" end
        res = res .. (c:match("[%g%s]") or "\x1b[31m.\x1b[39m")
    end
    return res
end

---
local dataExpanders = {}

local mcnetTransportTypes = {
    ["I"] = function(localAddress, remoteAddress, data)
        print("\t>>> ICMP")
    end,
    ["T"] = function(localAddress, remoteAddress, data)
        print("\t>>> TCP port=")
    end,
    ["D"] = function(localAddress, remoteAddress, data)
        print("\t>>> UDP port=")
    end,
}

local mcnetTypes = {
    ["H"] = function(localAddress, remoteAddress, data)
        local ttl, age, dist, n = string.unpack(">BHB", data)
        local host = data:sub(n)
        print("\t>> L2 mcnet HOST_FOUND ttl=" .. ttl .. " age=" .. age .. " dist=" .. dist .. " host=" .. normLine(host))
    end,
    ["R"] = function(localAddress, remoteAddress, data)
        local ttl, dest, age = string.unpack(">Bs1H", data)
        print("\t>> L2 mcnet SEEK_ROUTE ttl=" .. ttl .. " age=" .. age .. " dest=" .. normLine(dest))
    end,
    ["D"] = function(localAddress, remoteAddress, data)
        local ttl = string.unpack(">B", data)
        print("\t>> L2 mcnet DIRECT_DATA ttl=" .. ttl)
        if mcnetTransportTypes[data:sub(2,2)] then
            mcnetTransportTypes[data:sub(2,2)](localAddress, remoteAddress, data:sub(3))
        else
            print("\t\t>> L2 Unknown mcnet packet")
        end
    end,
    ["E"] = function(localAddress, remoteAddress, data)
        local ttl, dest, orig, dstart = string.unpack(">Bs1s1", data)
        local dat = data:sub(dstart)
        print("\t>> L2 mcnet ROUTED_DATA ttl=" .. ttl .. " dest=" .. normLine(dest) .. " origin=" .. orig)
        if mcnetTransportTypes[dat:sub(1,1)] then
            mcnetTransportTypes[dat:sub(1,1)](localAddress, remoteAddress, dat:sub(2))
        else
            print("\t\t>> L2 Unknown mcnet packet")
        end
    end,
}

local ethTypes = {
    ["\0"] = function(localAddress, remoteAddress, msg) --PKT_BEACON_OLD
        print("\t> L1 PKT_OLD_BEACON remote=" .. remoteAddress)
    end, 
    ["\32"] = function(localAddress, remoteAddress, msg) --PKT_BEACON
        print("\t> L1 PKT_BEACON remote=" .. remoteAddress)
    end, 
    ["\1"] = function(localAddress, remoteAddress, msg) --PKT_REGISTER
        print("\t> L1 PKT_REGISTER remote=" .. remoteAddress)
    end, 
    ["\2"] = function(localAddress, remoteAddress, msg) --PKT_REGISTER_ACK
        print("\t> L1 PKT_REGISTER_ACK remote=" .. remoteAddress)
    end, 
    ["\4"] = function(localAddress, remoteAddress, msg) --PKT_DATA
        print("\t> L1 PKT_DATA remote=" .. remoteAddress)
        if mcnetTypes[msg:sub(1,1)] then
            mcnetTypes[msg:sub(1,1)](localAddress, remoteAddress, msg:sub(2))
        else
            print("\t>> L2 Unknown mcnet packet "..normLine(msg:sub(1,1)).."("..string.unpack("B",msg)..")")
        end
    end, 
    ["\3"] = function(localAddress, remoteAddress, msg) --PKT_QUIT
        print("\t> L1 PKT_QUIT remote=" .. remoteAddress)
    end, 
    
}

function dataExpanders.modem_message(_, localAddress, remoteAddress, port, distance, msg)
    print("\tport=" .. port .. ", distance=" .. distance)
    if port == 1 then
        if ethTypes[msg:sub(1,1)] then
            ethTypes[msg:sub(1,1)](localAddress, remoteAddress, msg:sub(2))
        else
            print("\t> Unknown/OldNetwork modem message")
        end
    end
    io.write("\n")
end

---

io.write("Press 'Ctrl-C' to exit\n")
--pcall(function()
  repeat
    if #args > 0 then
      evt = table.pack(event.pullMultiple("interrupted", table.unpack(args)))
    else
      evt = table.pack(event.pull())
    end
    if evt[1] then
        io.write("\x1b[31m[" .. os.date("%T") .. "] \x1b[32m")
        io.write(tostring(evt[1]) .. string.rep(" ", math.max(10 - #tostring(evt[1]), 0) + 1))
        io.write("\x1b[33m" .. tostring(evt[2]) .. string.rep(" ", 37 - #tostring(evt[2])) .. "\x1b[39m")
        if evt.n > 2 then
          for i = 3, evt.n do
            io.write("  " .. normLine(tostring(evt[i])))
          end
        end
        io.write("\n")
        if dataExpanders[evt[1]] then
            dataExpanders[evt[1]](table.unpack(evt))
        end
    end
  until evt[1] == "interrupted"
--end)



