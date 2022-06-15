local component = require("component")
local sides = require("sides")

local robot = {}

-------------------------------------------------------------------------------
-- General

robot.component = component.robot

function robot.name()
    interrupt()
    return component.robot.name()
end

function robot.level()
    interrupt()
    if component.isAvailable("experience") then
        local level = 0
        for address in component.list("experience") do
            level = level + component.invoke(address, "level")
        end
        return level
    else
        return 0
    end
end

function robot.getLightColor()
    interrupt()
    return component.robot.getLightColor()
end

function robot.setLightColor(value)
    interrupt()
    return component.robot.setLightColor(value)
end

-------------------------------------------------------------------------------
-- World

function robot.detect()
    interrupt()
    return component.robot.detect(sides.front)
end

function robot.detectUp()
    interrupt()
    return component.robot.detect(sides.up)
end

function robot.detectDown()
    interrupt()
    return component.robot.detect(sides.down)
end

-------------------------------------------------------------------------------
-- Inventory

function robot.inventorySize()
    interrupt()
    return component.robot.inventorySize()
end

function robot.select(...)
    interrupt()
    return component.robot.select(...)
end

function robot.count(...)
    interrupt()
    return component.robot.count(...)
end

function robot.space(...)
    interrupt()
    return component.robot.space(...)
end

function robot.compareTo(...)
    interrupt()
    return component.robot.compareTo(...)
end

function robot.transferTo(...)
    interrupt()
    return component.robot.transferTo(...)
end

-------------------------------------------------------------------------------
-- Inventory + World

function robot.compare(fuzzy)
    interrupt()
    return component.robot.compare(sides.front, fuzzy)
end

function robot.compareUp(fuzzy)
    interrupt()
    return component.robot.compare(sides.up, fuzzy)
end

function robot.compareDown(fuzzy)
    interrupt()
    return component.robot.compare(sides.down, fuzzy)
end

function robot.drop(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drop(sides.front, count)
end

function robot.dropUp(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drop(sides.up, count)
end

function robot.dropDown(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drop(sides.down, count)
end

function robot.place(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.place(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeUp(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.place(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.placeDown(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.place(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.suck(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.suck(sides.front, count)
end

function robot.suckUp(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.suck(sides.up, count)
end

function robot.suckDown(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.suck(sides.down, count)
end

-------------------------------------------------------------------------------
-- Tool

function robot.durability()
    interrupt()
    return component.robot.durability()
end

function robot.swing(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.swing(sides.front, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingUp(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.swing(sides.up, side, sneaky ~= nil and sneaky ~= false)
end

function robot.swingDown(side, sneaky)
    checkArg(1, side, "nil", "number")
    interrupt()
    return component.robot.swing(sides.down, side, sneaky ~= nil and sneaky ~= false)
end

function robot.use(side, sneaky, duration)
    checkArg(1, side, "nil", "number")
    checkArg(3, duration, "nil", "number")
    interrupt()
    return component.robot.use(sides.front, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useUp(side, sneaky, duration)
    checkArg(1, side, "nil", "number")
    checkArg(3, duration, "nil", "number")
    interrupt()
    return component.robot.use(sides.up, side, sneaky ~= nil and sneaky ~= false, duration)
end

function robot.useDown(side, sneaky, duration)
    checkArg(1, side, "nil", "number")
    checkArg(3, duration, "nil", "number")
    interrupt()
    return component.robot.use(sides.down, side, sneaky ~= nil and sneaky ~= false, duration)
end

-------------------------------------------------------------------------------
-- Movement

function robot.forward()
    interrupt()
    return component.robot.move(sides.front)
end

function robot.back()
    interrupt()
    return component.robot.move(sides.back)
end

function robot.up()
    interrupt()
    return component.robot.move(sides.up)
end

function robot.down()
    interrupt()
    return component.robot.move(sides.down)
end

function robot.turnLeft()
    interrupt()
    return component.robot.turn(false)
end

function robot.turnRight()
    interrupt()
    return component.robot.turn(true)
end

function robot.turnAround()
    interrupt()
    local turn = math.random() < 0.5 and robot.turnLeft or robot.turnRight
    return turn() and turn()
end

-------------------------------------------------------------------------------
-- Tank

function robot.tankCount()
    interrupt()
    return component.robot.tankCount()
end

function robot.selectTank(tank)
    interrupt()
    return component.robot.selectTank(tank)
end

function robot.tankLevel(...)
    interrupt()
    return component.robot.tankLevel(...)
end

function robot.tankSpace(...)
    interrupt()
    return component.robot.tankSpace(...)
end

function robot.compareFluidTo(...)
    interrupt()
    return component.robot.compareFluidTo(...)
end

function robot.transferFluidTo(...)
    interrupt()
    return component.robot.transferFluidTo(...)
end

-------------------------------------------------------------------------------
-- Tank + World

function robot.compareFluid()
    interrupt()
    return component.robot.compareFluid(sides.front)
end

function robot.compareFluidUp()
    interrupt()
    return component.robot.compareFluid(sides.up)
end

function robot.compareFluidDown()
    interrupt()
    return component.robot.compareFluid(sides.down)
end

function robot.drain(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drain(sides.front, count)
end

function robot.drainUp(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drain(sides.up, count)
end

function robot.drainDown(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.drain(sides.down, count)
end

function robot.fill(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.fill(sides.front, count)
end

function robot.fillUp(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.fill(sides.up, count)
end

function robot.fillDown(count)
    checkArg(1, count, "nil", "number")
    interrupt()
    return component.robot.fill(sides.down, count)
end

-------------------------------------------------------------------------------

return robot