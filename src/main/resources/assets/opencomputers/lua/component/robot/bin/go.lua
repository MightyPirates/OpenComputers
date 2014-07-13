local robot=require("robot")
local shell=require("shell")
local args = shell.parse(...)
if #args<1 then
  io.stderr:write("No direction specified\n")
  return
end
if args[1] == "forward" then
  robot.forward()
elseif args[1] == "back" then
  robot.back()
elseif args[1]=="left" then
  robot.turnLeft()
elseif args[1]=="right" then
  robot.turnRight()
elseif args[1]=="up" then
  robot.up()
elseif args[1]=="down" then
  robot.down()
else
  io.stderr:write(args[1]..": not a valid direction!\n")
  return
end
