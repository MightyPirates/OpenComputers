local component = require("component")
local computer = require("computer")
local robot = require("robot")
local shell = require("shell")
local sides = require("sides")

if not component.isAvailable("robot") then
  io.stderr:write("can only run on robots")
  return
end

local args, options = shell.parse(...)
if #args < 1 then
  io.write("Usage: dig [-s] <size>\n")
  io.write(" -s: shutdown when done.")
  return
end

local size = tonumber(args[1])
if not size then
  io.stderr:write("invalid size")
  return
end

local r = component.robot
local x, y, z, f = 0, 0, 0, 0
local dropping = false -- avoid recursing into drop()
local delta = {[0] = function() x = x + 1 end, [1] = function() y = y + 1 end,
               [2] = function() x = x - 1 end, [3] = function() y = y - 1 end}

local function turnRight()
  robot.turnRight()
  f = (f + 1) % 4
end

local function turnLeft()
  robot.turnLeft()
  f = (f - 1) % 4
end

local function turnTowards(side)
  if f == side - 1 then
    turnRight()
  else
    while f ~= side do
      turnLeft()
    end
  end
end

local checkedDrop -- forward declaration

local function clearBlock(side, cannotRetry)
  while r.suck(side) do
    checkedDrop()
  end
  local result, reason = r.swing(side)
  if result then
    checkedDrop()
  else
    local _, what = r.detect(side)
    if cannotRetry and what ~= "air" and what ~= "entity" then
      return false
    end
  end
  return true
end

local function tryMove(side)
  side = side or sides.forward
  local tries = 10
  while not r.move(side) do
    tries = tries - 1
    if not clearBlock(side, tries < 1) then
      return false
    end
  end
  if side == sides.down then
    z = z + 1
  elseif side == sides.up then
    z = z - 1
  else
    delta[f]()
  end
  return true
end

local function moveTo(tx, ty, tz, backwards)
  local axes = {
    function()
      while z > tz do
        tryMove(sides.up)
      end
      while z < tz do
        tryMove(sides.down)
      end
    end,
    function()
      if y > ty then
        turnTowards(3)
        repeat tryMove() until y == ty
      elseif y < ty then
        turnTowards(1)
        repeat tryMove() until y == ty
      end
    end,
    function()
      if x > tx then
        turnTowards(2)
        repeat tryMove() until x == tx
      elseif x < tx then
        turnTowards(0)
        repeat tryMove() until x == tx
      end
    end
  }
  if backwards then
    for axis = 3, 1, -1 do
      axes[axis]()
    end
  else
    for axis = 1, 3 do
      axes[axis]()
    end
  end
end

function checkedDrop(force)
  local empty = 0
  for slot = 1, 16 do
    if robot.count(slot) == 0 then
      empty = empty + 1
    end
  end
  if not dropping and empty == 0 or force and empty < 16 then
    local ox, oy, oz, of = x, y, z, f
    dropping = true
    moveTo(0, 0, 0)
    turnTowards(2)

    for slot = 1, 16 do
      if robot.count(slot) > 0 then
        robot.select(slot)
        local wait = 1
        repeat
          if not robot.drop() then
            os.sleep(wait)
            wait = math.min(10, wait + 1)
          end
        until robot.count(slot) == 0
      end
    end
    robot.select(1)

    dropping = false
    moveTo(ox, oy, oz, true)
    turnTowards(of)
  end
end

local function step()
  clearBlock(sides.down)
  if not tryMove() then
    return false
  end
  clearBlock(sides.up)
  return true
end

local function turn(i)
  if i % 2 == 1 then
    turnRight()
  else
    turnLeft()
  end
end

local function digLayer()
  --[[ We move in zig-zag lines, clearing three layers at a time. This means we
       have to differentiate at the end of the last line between even and odd
       sizes on which way to face for the next layer:
       For either size we rotate once to the right. For even sizes this will
       cause the next layer to be dug out rotated by ninety degrees. For odd
       ones the return path is symmetrical, meaning we just turn around.

       Examples for two layers:

       s--x--x      e--x--x      s--x--x--x      x--x  x--x
             |            |               |      |  |  |  |
       x--x--x  ->  x--x--x      x--x--x--x      x  x  x  x
       |            |            |           ->  |  |  |  |
       x--x--e      x--x--s      x--x--x--x      x  x  x  x
                                          |      |  |  |  |
                                 e--x--x--x      s  x--x  e

       Legend: s = start, x = a position, e = end, - = a move
  ]]
  for i = 1, size do
    for j = 1, size - 1 do
      if not step() then
        return false
      end
    end
    if i < size then
      -- End of a normal line, move the "cap".
      turn(i)
      if not step() then
        return false
      end
      turn(i)
    else
      turnRight()
      if size % 2 == 1 then
        turnRight()
      end
      for i = 1, 3 do
        if not tryMove(sides.down) then
          return false
        end
      end
    end
  end
  return true
end

repeat until not digLayer()
moveTo(0, 0, 0)
turnTowards(0)
checkedDrop(true)

if options.s then
  computer.shutdown()
end