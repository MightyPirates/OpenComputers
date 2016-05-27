local network = require "network"
local computer = require "computer"
local args = {...}

local function align(txt)return txt .. ("        "):sub(#txt+1)end

if #args < 1 then
    print("Network interfaces:")
    local info = network.info.getInfo()
    for node, info in pairs(info.interfaces)do
        print(align(node).."Link encap:"..info.linkName)
        print("        HWaddr "..info.selfAddr)
        if node == "lo" then print("        HWaddr "..computer.address()) end
        local pktIn, pktOut, bytesIn, bytesOut = network.info.getInterfaceInfo(node)
        print("        RX packets:"..tostring(pktIn))
        print("        TX packets:"..tostring(pktOut))
        print("        RX bytes:"..tostring(bytesIn).."  TX bytes:"..tostring(bytesOut))
    end
elseif args[1] == "bind" and args[2] then
    print("Address attached")
    network.ip.bind(args[2])
else
   print("Usage:")
   print(" ifconfig - view network summary")
   print(" ifconfig bind [addr] - 'attach' addnitional address to computer")
end






