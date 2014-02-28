--[[ Makes the robot refuel itself using fuel from the inventory and lets you get the current fuel count.
     Author: Vexatos]]
local component = require("component")
local robot = require("robot")
local shell = require("shell")

-- Not using shell.parse to allow `refuel 1 -10`, the -10 would be
-- parsed as an option otherwise.
local args = {...}

local function printUsage()
  print("Usages:")
  print("refuel")
  print("  Gets the current fuel count.")
  print("refuel <slot> [amount]")
  print("  Refuel the specified number of items (default")
  print("  as many as possible) from the specified slot.")
  print("refuel all")
  print("  Refuel from all slots.")
end

if component.isAvailable("generator") then
  local g = component.generator
  if #args == 0 then
    print("Current number of items in generator: "..g.count())
  elseif tonumber(args[1]) ~= nil then
    local slot = tonumber(args[1])
    local count = tonumber(args[2]) or 64
    robot.select(slot)
    if count > 0 then
      io.write("Refueling from slot "..slot.."... ")
      local success, msg = g.insert(count)
      if success then
        print("success.")
      else
        print("failed: "..msg)
      end
    elseif count < 0 then
      io.write("Ejecting into slot "..slot.."... ")
      if g.remove(-count) then
        print("success.")
      else
        print("failed.")
      end
    end -- else: ignore zero
    robot.select(1)
  elseif string.lower(args[1]) == "all" then
    io.write("Refueling from all slots... ")
    for i = 1, 16 do
      robot.select(i)
      g.insert()
    end
    robot.select(1)
    print("done.")
  else
    printUsage()
  end
else
  print("This program requires the generator upgrade to be installed.")
end
