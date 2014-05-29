local component = require("component")
local sides = require("sides")

local robot = {}

-------------------------------------------------------------------------------
-- General

function robot.name()
  return component.robot.name()
end

function robot.level()
  if component.isAvailable("experience") then
    return component.experience.level()
  else
    return 0
  end
end

-------------------------------------------------------------------------------
-- World

function robot.detect()
  return component.robot.detect(sides.front)
end

function robot.detectUp()
  return component.robot.detect(sides.up)
end

function robot.detectDown()
  return component.robot.detect(sides.down)
end

-------------------------------------------------------------------------------
-- Inventory

function robot.inventorySize()
  return component.robot.inventorySize()
end


function robot.select(slot)
  return component.robot.select(slot)
end

function robot.count(slot)
  return component.robot.count(slot)
end

function robot.space(slot)
  return component.robot.space(slot)
end

function robot.compareTo(slot)
  return component.robot.compareTo(slot)
end

function robot.transferTo(slot, count)
  return component.robot.transferTo(slot, count)
end

-------------------------------------------------------------------------------
-- Inventory + World

function robot.compare()
  return component.robot.compare(sides.front)
end

function robot.compareUp()
  return component.robot.compare(sides.up)
end

function robot.compareDown()
  return component.robot.compare(sides.down)
end

function robot.drop(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drop(sides.front, count)
end

function robot.dropUp(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drop(sides.up, count)
end

function robot.dropDown(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drop(sides.down, count)
end

function robot.place(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.place(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.place(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.place(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.suck(count)
  checkArg(1, count, "nil", "number")
  return component.robot.suck(sides.front, count)
end

function robot.suckUp(count)
  checkArg(1, count, "nil", "number")
  return component.robot.suck(sides.up, count)
end

function robot.suckDown(count)
  checkArg(1, count, "nil", "number")
  return component.robot.suck(sides.down, count)
end

-------------------------------------------------------------------------------
-- Tool

function robot.durability()
  return component.robot.durability()
end


function robot.swing(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.swing(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.swing(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.robot.swing(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.use(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.robot.use(sides.front, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useUp(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.robot.use(sides.up, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useDown(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.robot.use(sides.down, side, sneaky ~= nil and sneaky ~= false, duration)
end

-------------------------------------------------------------------------------
-- Movement

function robot.forward()
  return component.robot.move(sides.front)
end

function robot.back()
  return component.robot.move(sides.back)
end

function robot.up()
  return component.robot.move(sides.up)
end

function robot.down()
  return component.robot.move(sides.down)
end


function robot.turnLeft()
  return component.robot.turn(false)
end

function robot.turnRight()
  return component.robot.turn(true)
end

function robot.turnAround()
  local turn = math.random() < 0.5 and robot.turnLeft or robot.turnRight
  return turn() and turn()
end

-------------------------------------------------------------------------------

return robot
