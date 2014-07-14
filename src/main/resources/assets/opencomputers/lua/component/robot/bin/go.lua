--[[ go, makes the robot go a specified number of blocks in a certain direction or turn around.
Author: Vexatos
]]
local robot=require("robot")
local shell=require("shell")
local args = shell.parse(...)
if #args<1 then
  print("'go' - Makes the robot go in a certain direction")
  print("Usage:")
  print("'go forward [number]' to make the robot go forward a number of blocks (defaults to 1)")
  print("'go back [number]' to make the robot go backwards")
  print("'go up [number]' to make the robot go upwards")
  print("'go down [number]' to make the robot go downwards")
  print("'go left [number]' to make the robot turn left a number of times")
  print("'go right [number]' to make the robot turn right a number of times")
  return
end
local distance = args[2] or 1

if not tonumber(distance) or tonumber(distance) <= 0 then
  io.stderr:write(distance..": not a positive number!\n")
  return
end

distance = math.floor(tonumber(distance))
local action

if args[1] == "forward" then
  action = robot.forward
elseif args[1] == "back" then
  action = robot.back
elseif args[1] == "left" then
  action = robot.turnLeft
elseif args[1] == "right" then
  action = robot.turnRight
elseif args[1] == "up" then
  action = robot.up
elseif args[1] == "down" then
  action = robot.down
else
  io.stderr:write(args[1]..": not a valid direction!\n")
  return
end

for i = 1,distance do
  action()
end
