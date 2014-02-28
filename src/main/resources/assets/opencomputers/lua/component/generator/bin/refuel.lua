--[[ Makes the robot refuel itself using fuel from the inventory and lets you get the current fuel count.
     Author: Vexatos]]
local component = require("component")
local shell = require("shell")

local args = shell.parse(...)

local function printUsage()
  print("Usages:")
  print("'refuel' to get the current fuel count")
  print("'refuel <slot>' to refuel from that specific slot")
  print("'refuel all' to refuel from all slots")
end

if component.isAvailable("generator") then
  local g = component.generator
  if #args == 0 then
    print("Current Number of items in generator: "..g.count())
  elseif tonumber(args[1]) ~= nil then
    print("Refuelling from slot"..args[1].."...")
    local success, msg = g.insert(tonumber(args[1]))
    if success then
      print("Success.")
    else
      print("Error: "..msg)
    end
  elseif string.lower(args[1]) == "all" then
    io.write("Refuelling from all slots...")
    for i = 1, 16 do
      g.insert(i)
    end
    print("Done.")
  else
    printUsage()
  end
else
  print("This program requires the generator upgrade to be installed.")
end
