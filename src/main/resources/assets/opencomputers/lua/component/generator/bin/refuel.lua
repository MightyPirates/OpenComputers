--[[ Makes the robot refuel itself using fuel from the inventory and lets you get the current fuel count.
     Author: Vexatos]]
local component = require("component")
local robot = require("robot")
local shell = require("shell")

local args = shell.parse(...)

local function printUsage()
  print("Usages:")
  print("'refuel' to get the current fuel count")
  print("'refuel <slot> [amount]' to refuel [amount] from that specific slot,\n  or try to completely fill it")
  print("'refuel all' to refuel from all slots")
end

if component.isAvailable("generator") then
  local g = component.generator
  if #args == 0 then
    print("Current Number of items in generator: "..g.count())
  elseif tonumber(args[1]) ~= nil then
    print("Refuelling from slot"..args[1].."...")
    robot.select(tonumber(args[1]))
    local success, msg
    if tonumber(args[2]) ~= nil then
      if tonumber(args[2]) > 0 then
        success, msg = g.insert(tonumber(args[2]))
      elseif tonumber(args[2]) < 0 then
        success = g.remove(math.abs(tonumber(args[2])))
        if not (success == true) then msg = "Could not remove item, generator is empty" end
      else
        msg = "You can't insert 0 of an item!"
      end
    else
      success, msg = g.insert()
    end
    if success then
      print("Success.")
    else
      print("Error: "..msg)
    end
    robot.select(1)
  elseif string.lower(args[1]) == "all" then
    io.write("Refuelling from all slots...")
    for i = 1, 16 do
      robot.select(i)
      g.insert()
    end
    robot.select(1)
    print("Done.")
  else
    printUsage()
  end
else
  print("This program requires the generator upgrade to be installed.")
end
