local colors = require("colors")
local component = require("component")
local shell = require("shell")
local sides = require("sides")

if not component.isAvailable("redstone") then
  io.stderr:write("This program requires a redstone card or redstone I/O block.")
  return
end
local rs = component.redstone

local args, options = shell.parse(...)
if #args < 1 then
  if rs.setBundledOutput then
    io.write("Usage: redstone <side> [-b <color>] [<value>]")
  else
    io.write("Usage: redstone <side> [<value>]")
  end
  return
end

local side = sides[args[1]]
if not side then
  io.stderr:write("invalid side")
  return
end
if type(side) == "string" then
  side = sides[side]
end

if options.b then
  if not rs.setBundledOutput then
    io.stderr:write("bundled redstone not available")
    return
  end
  local color = colors[args[2]]
  if not color then
    io.stderr:write("invalid color")
    return
  end
  if type(color) == "string" then
    color = colors[color]
  end
  if #args > 2 then
    local value = args[3]
    if tonumber(value) then
      value = tonumber(value)
    else
      value = ({["true"]=true,["on"]=true,["yes"]=true})[value] and 255 or 0
    end
    rs.setBundledOutput(side, color, value)
  end
  io.write("in: ", rs.getBundledInput(side, color), "\n")
  io.write("out: ", rs.getBundledOutput(side, color))
else
  if #args > 1 then
    local value = args[2]
    if tonumber(value) then
      value = tonumber(value)
    else
      value = ({["true"]=true,["on"]=true,["yes"]=true})[value] and 15 or 0
    end
    rs.setOutput(side, value)
  end
  io.write("in: ", rs.getInput(side), "\n")
  io.write("out: ", rs.getOutput(side))
end
