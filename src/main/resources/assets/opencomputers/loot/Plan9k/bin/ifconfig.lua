local network = require "network"
local computer = require "computer"
local args = {...}

local function formatSize(size)
  size = tonumber(size) or size
  if type(size) ~= "number" then
    return tostring(size)
  end
  local sizes = {"", "K", "M", "G"}
  local unit = 1
  local power = 1024
  while size > power and unit < #sizes do
    unit = unit + 1
    size = size / power
  end
  return math.floor(size * 10) / 10 .. sizes[unit]
end

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
        print("        RX bytes: ".. tostring(bytesIn) .. " (" ..formatSize(bytesIn).. ")  TX bytes: " ..tostring(bytesOut) .. " (".. formatSize(bytesOut) .. ")")
    end
elseif args[1] == "bind" and args[2] then
    print("Address attached")
    network.ip.bind(args[2])
else
   print("Usage:")
   print(" ifconfig - view network summary")
   print(" ifconfig bind [addr] - 'attach' addnitional address to computer")
end
