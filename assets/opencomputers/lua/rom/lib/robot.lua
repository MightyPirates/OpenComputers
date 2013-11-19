if not os.isRobot() then
  return
end
local function proxy()
  return component.computer
end
local robot = {}


function robot.select(index)
  return proxy().select(index)
end

function robot.count()
  return proxy().count()
end

function robot.space()
  return proxy().select()
end

function robot.compareTo(index)
  return proxy().compareTo(index)
end

function robot.transferTo(index, count)
  return proxy().transferTo(index, count)
end

function robot.compare()
  return proxy().compare(sides.front)
end

function robot.compareUp()
  return proxy().compare(sides.up)
end

function robot.compareDown()
  return proxy().compare(sides.down)
end

function robot.drop(count)
  checkArg(1, count, "nil", "number")
  return proxy().drop(sides.front, count)
end

function robot.dropUp(count)
  checkArg(1, count, "nil", "number")
  return proxy().drop(sides.up, count)
end

function robot.dropDown(count)
  checkArg(1, count, "nil", "number")
  return proxy().drop(sides.down, count)
end

function robot.place(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().place(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().place(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().place(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.suck(count)
  checkArg(1, count, "nil", "number")
  return proxy().suck(sides.front, count)
end

function robot.suckUp(count)
  checkArg(1, count, "nil", "number")
  return proxy().suck(sides.up, count)
end

function robot.suckDown(count)
  checkArg(1, count, "nil", "number")
  return proxy().suck(sides.down, count)
end


function robot.detect()
  return proxy().detect(sides.front)
end

function robot.detectUp()
  return proxy().detect(sides.up)
end

function robot.detectDown()
  return proxy().detect(sides.down)
end


function robot.swing(side)
  checkArg(1, side, "nil", "number")
  return proxy().swing(sides.front, side)
end

function robot.swingUp(side)
  checkArg(1, side, "nil", "number")
  return proxy().swing(sides.up, side)
end

function robot.swingDown(side)
  checkArg(1, side, "nil", "number")
  return proxy().swing(sides.down, side)
end

function robot.use(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().use(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.useUp(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().use(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.useDown(side, sneaky)
  checkArg(1, side, "nil", "number")
  return proxy().use(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.durability()
  return proxy().durability()
end


function robot.forward()
  return proxy().move(sides.front)
end

function robot.back()
  return proxy().move(sides.back)
end

function robot.up()
  return proxy().move(sides.up)
end

function robot.down()
  return proxy().move(sides.down)
end

function robot.turnLeft()
  return proxy().turn(false)
end

function robot.turnRight()
  return proxy().turn(true)
end

_G.robot = robot