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

function robot.getLightColor()
  return component.robot.getLightColor()
end

function robot.setLightColor(value)
  return component.robot.setLightColor(value)
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


function robot.select(...)
  return component.robot.select(...)
end

function robot.count(...)
  return component.robot.count(...)
end

function robot.space(...)
  return component.robot.space(...)
end

function robot.compareTo(...)
  return component.robot.compareTo(...)
end

function robot.transferTo(...)
  return component.robot.transferTo(...)
end

-------------------------------------------------------------------------------
-- Inventory + World

function robot.compare(fuzzy)
  return component.robot.compare(sides.front, fuzzy)
end

function robot.compareUp(fuzzy)
  return component.robot.compare(sides.up, fuzzy)
end

function robot.compareDown(fuzzy)
  return component.robot.compare(sides.down, fuzzy)
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
-- Tank

function robot.tankCount()
  return component.robot.tankCount()
end


function robot.selectTank(tank)
  return component.robot.selectTank(tank)
end

function robot.tankLevel(...)
  return component.robot.tankLevel(...)
end

function robot.tankSpace(...)
  return component.robot.tankSpace(...)
end

function robot.compareFluidTo(...)
  return component.robot.compareFluidTo(...)
end

function robot.transferFluidTo(...)
  return component.robot.transferFluidTo(...)
end

-------------------------------------------------------------------------------
-- Tank + World

function robot.compareFluid()
  return component.robot.compareFluid(sides.front)
end

function robot.compareFluidUp()
  return component.robot.compareFluid(sides.up)
end

function robot.compareFluidDown()
  return component.robot.compareFluid(sides.down)
end

function robot.drain(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drain(sides.front, count)
end

function robot.drainUp(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drain(sides.up, count)
end

function robot.drainDown(count)
  checkArg(1, count, "nil", "number")
  return component.robot.drain(sides.down, count)
end

function robot.fill(count)
  checkArg(1, count, "nil", "number")
  return component.robot.fill(sides.front, count)
end

function robot.fillUp(count)
  checkArg(1, count, "nil", "number")
  return component.robot.fill(sides.up, count)
end

function robot.fillDown(count)
  checkArg(1, count, "nil", "number")
  return component.robot.fill(sides.down, count)
end

-------------------------------------------------------------------------------

return robot
