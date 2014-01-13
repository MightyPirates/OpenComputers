if not computer.isRobot() then
  return
end

local robot = {}

-------------------------------------------------------------------------------
-- General

function robot.level()
  return component.computer.level()
end

-------------------------------------------------------------------------------
-- World

function robot.detect()
  return component.computer.detect(sides.front)
end

function robot.detectUp()
  return component.computer.detect(sides.up)
end

function robot.detectDown()
  return component.computer.detect(sides.down)
end

-------------------------------------------------------------------------------
-- Inventory

function robot.select(slot)
  return component.computer.select(slot)
end

function robot.count(slot)
  return component.computer.count(slot)
end

function robot.space(slot)
  return component.computer.space(slot)
end

function robot.compareTo(slot)
  return component.computer.compareTo(slot)
end

function robot.transferTo(slot, count)
  return component.computer.transferTo(slot, count)
end

-------------------------------------------------------------------------------
-- Inventory + World

function robot.compare()
  return component.computer.compare(sides.front)
end

function robot.compareUp()
  return component.computer.compare(sides.up)
end

function robot.compareDown()
  return component.computer.compare(sides.down)
end

function robot.drop(count)
  checkArg(1, count, "nil", "number")
  return component.computer.drop(sides.front, count)
end

function robot.dropUp(count)
  checkArg(1, count, "nil", "number")
  return component.computer.drop(sides.up, count)
end

function robot.dropDown(count)
  checkArg(1, count, "nil", "number")
  return component.computer.drop(sides.down, count)
end

function robot.place(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.place(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.place(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.place(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.suck(count)
  checkArg(1, count, "nil", "number")
  return component.computer.suck(sides.front, count)
end

function robot.suckUp(count)
  checkArg(1, count, "nil", "number")
  return component.computer.suck(sides.up, count)
end

function robot.suckDown(count)
  checkArg(1, count, "nil", "number")
  return component.computer.suck(sides.down, count)
end

-------------------------------------------------------------------------------
-- Tool

function robot.durability()
  return component.computer.durability()
end


function robot.swing(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.swing(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.swing(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return component.computer.swing(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.use(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.computer.use(sides.front, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useUp(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.computer.use(sides.up, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useDown(side, sneaky, duration)
  checkArg(1, side, "nil", "number")
  checkArg(3, duration, "nil", "number")
  return component.computer.use(sides.down, side, sneaky ~= nil and sneaky ~= false, duration)
end

-------------------------------------------------------------------------------
-- Movement

function robot.forward()
  return component.computer.move(sides.front)
end

function robot.back()
  return component.computer.move(sides.back)
end

function robot.up()
  return component.computer.move(sides.up)
end

function robot.down()
  return component.computer.move(sides.down)
end


function robot.turnLeft()
  return component.computer.turn(false)
end

function robot.turnRight()
  return component.computer.turn(true)
end

function robot.turnAround()
  local turn = math.random() < 0.5 and robot.turnLeft or robot.turnRight
  return turn() and turn()
end


_G.robot = robot