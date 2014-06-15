component = require("component")
if component.isAvailable("robot") == false then
  print("Error! This program works only on robots") return
end

computer = require("computer")
robot    = require("robot")
term     = require("term")

xDiff    = {0, 1, 0, -1}
zDiff    = {1, 0, -1, 0}
globalOrient  = 1
global_xCoord = 1
global_zCoord = 1

local input = { ... }

mazeRoute    = {}
mazeGrid     = {}
mazeWidth    = tonumber(input[1]) or 0
mazeLength   = tonumber(input[2]) or 0
cellHeight   = tonumber(input[3]) or 1
cellDiameter = tonumber(input[4]) or 1
autoRefuel   = input[5] or "false"


if mazeWidth < 2 or mazeLength < 2 or cellHeight < 1 or cellDiameter < 1 or autoRefuel ~= "true" and autoRefuel ~= "false" then
  print("Usage: maze <int:width> <int:length> <int:cellHeight> <int:cellDiameter> <bool:refuel>")
  return
end

if component.isAvailable("generator") == false and autoRefuel == "true" then 
  print("Error! Missing Generator Upgrade")
  return
end

term.clear() term.setCursor(1, 1)
print("Dynamic Maze Generator using Depth-First search")
print("I heard you like mazes ;)")
print("AutoRefuel = "..autoRefuel) print("Running...")

for x = 1, mazeWidth do
  mazeGrid[x] = {}

  for z = 1, mazeLength do
    mazeGrid[x][z] = false
  end
end

function refuel()
  if component.generator.count() > 0 then return end
  
  for i = 1, 16 do
    robot.select(i)
    if component.generator.insert(16) then break end
  end
end

function left()
  robot.turnLeft()
  if globalOrient == 1 then
    globalOrient = 4
  else
    globalOrient = globalOrient - 1
  end
end

function right()
  robot.turnRight()
  if globalOrient == 4 then
    globalOrient = 1
  else
    globalOrient = globalOrient + 1
  end
end

function depthFirst()
  local dir, temp_xCoord, temp_zCoord
  local counter = { false, false, false, false }

  repeat
    dir = math.random(4)
    temp_xCoord = global_xCoord; temp_zCoord = global_zCoord

    if dir == 1 then
      if global_zCoord < mazeLength then
        temp_zCoord = temp_zCoord + 1
      end
    elseif dir == 2 then
      if global_xCoord < mazeWidth then
        temp_xCoord = temp_xCoord + 1
      end
    elseif dir == 3 then
      if global_zCoord > 1 then
        temp_zCoord = temp_zCoord - 1
      end
    elseif dir == 4 then
      if global_xCoord > 1 then
        temp_xCoord = temp_xCoord - 1
      end
    end

    for i = 1, 4 do
      if counter[i] == false then
        counter[dir] = true
        break
      elseif counter[dir] == true and i == 4 then
        if trackBack() == false then
          return false
        else
          counter = { false, false, false, false }
        end
      end
    end
  until checkNextCell(temp_xCoord, temp_zCoord, dir) == true

  mazeGrid[temp_xCoord][temp_zCoord] = true
  gotoCell(dir) clearCell(cellDiameter, cellHeight)
  return true

end

function clearCell(x, y)
  local yCoord = 1

  while yCoord <= y do
    local xCoord = 1
    local zCoord = 1
    local orient = 1

    while xCoord <= x and x > 1 do
      if zCoord == 1 then
        while orient ~= 1 do
          right()
          orient = orient%4 + 1
        end
      elseif zCoord == x then
        while orient ~= 3 do
          right()
          orient = orient%4 + 1
        end
      end

      repeat robot.swing() until robot.forward()
      zCoord = zCoord + zDiff[orient]

      if xCoord < x and zCoord == 1 or xCoord < x and zCoord == x then
        while orient ~= 2 do
          right()
          orient = orient%4 + 1
        end

        repeat robot.swing() until robot.forward()
        xCoord = xCoord + xDiff[orient]

      elseif zCoord == 1 or zCoord == x then
        break
      end
    end

    local c
    if cellDiameter/2 == math.floor(cellDiameter/2) then c = 4 else c = 3 end

    while orient ~= c and cellDiameter > 1 do
      right()
      orient = orient%4 + 1
    end

    if yCoord < y then
      repeat robot.swingUp() until robot.up()
      yCoord = yCoord + 1
    elseif xCoord == x then
      break
    end
  
  end

  while yCoord > 1 do
    if robot.down() then
      yCoord = yCoord - 1
    else
      robot.swingDown()
    end
  end
 
end

function gotoCell(dir)
  local xCoord = 1
  local zCoord = 1
  local orient = 1
  local tempOrient = globalOrient

  while globalOrient ~= dir do
    right()
    orient = orient%4 + 1
  end

  while xCoord >= 1 and xCoord <= cellDiameter and zCoord >= 1 and zCoord <= cellDiameter do
    if robot.forward() then
      xCoord = xCoord + xDiff[orient]
      zCoord = zCoord + zDiff[orient]
    else
      robot.swing()
    end
  end
 
  global_xCoord = global_xCoord + xDiff[dir]
  global_zCoord = global_zCoord + zDiff[dir]
 
  if cellDiameter == 1 then return end
 
  if tempOrient == 1 and dir == 1 or tempOrient == 2 and dir == 1 or tempOrient == 1 and dir == 2 or tempOrient == 4 and dir == 2 then
    while globalOrient ~= 1 do
      left()
    end
  elseif tempOrient == 1 and dir == 3 or tempOrient == 2 and dir == 2 or tempOrient == 2 and dir == 3 or tempOrient == 3 and dir == 2 then
    while globalOrient ~= 2 do
      left()
    end
  elseif tempOrient == 2 and dir == 4 or tempOrient == 3 and dir == 3 or tempOrient == 3 and dir == 4 or tempOrient == 4 and dir == 3 then
    while globalOrient ~= 3 do
      left()
    end
  elseif tempOrient == 1 and dir == 4 or tempOrient == 3 and dir == 1 or tempOrient == 4 and dir == 1 or tempOrient == 4 and dir == 4 then
    while globalOrient ~= 4 do
      left()
    end
  end
 
end

function checkNextCell(x, z, dir)
  local temp_xMath = {}
  local temp_zMath = {}

  if dir == 1 then
    temp_xMath = {  1, -1,  0,  1, -1 }
    temp_zMath = {  0,  0,  1,  1,  1 }

  elseif dir == 2 then
    temp_xMath = {  0,  0,  1,  1,  1 }
    temp_zMath = {  1, -1,  0,  1, -1 }

  elseif dir == 3 then
    temp_xMath = {  1, -1,  0,  1, -1 }
    temp_zMath = {  0,  0, -1, -1, -1 }

  elseif dir == 4 then
    temp_xMath = {  0,  0, -1, -1, -1 }
    temp_zMath = {  1, -1,  0,  1, -1 }
  end

  for i = 1, 5 do
    if x + temp_xMath[i] <= mazeWidth and x + temp_xMath[i] >= 1 and z + temp_zMath[i] <= mazeLength and z + temp_zMath[i] >= 1 then
      if mazeGrid[x][z] == true or mazeGrid[x+temp_xMath[i]][z+temp_zMath[i]] == true then
        return false
      end
    end
  end

  table.insert(mazeRoute, dir)
  return true

end

function trackBack()
  if #mazeRoute == 0 then return false end

  if mazeRoute[#mazeRoute] == 1 then
    gotoCell(3)
  elseif mazeRoute[#mazeRoute] == 2 then
    gotoCell(4)
  elseif mazeRoute[#mazeRoute] == 3 then
    gotoCell(1)
  elseif mazeRoute[#mazeRoute] == 4 then
    gotoCell(2)
  end

  table.remove(mazeRoute)
  return true
end

clearCell(cellDiameter, cellHeight)
mazeGrid[1][1] = true

repeat
  if computer.energy() < computer.maxEnergy()/1.43 and autoRefuel == "true" then refuel() end
until depthFirst() == false

term.setCursor(1, 4) term.clearLine()
print("Done!")
