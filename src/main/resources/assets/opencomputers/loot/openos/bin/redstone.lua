local colors = require("colors")
local component = require("component")
local shell = require("shell")
local sides = require("sides")

if not component.isAvailable("redstone") then
  io.stderr:write("This program requires a redstone card or redstone I/O block.\n")
  return 1
end
local rs = component.redstone

local args, options = shell.parse(...)
if #args == 0 and not options.w and not options.f then
  io.write("Usage:\n")
  io.write("  redstone <side> [<value>]\n")
  if rs.setBundledOutput then
    io.write("  redstone -b <side> <color> [<value>]\n")
  end
  if rs.setWirelessOutput then
    io.write("  redstone -w [<value>]\n")
    io.write("  redstone -f [<frequency>]\n")
  end
  return
end

if options.w then
  if not rs.setWirelessOutput then
    io.stderr:write("wireless redstone not available\n")
    return 1
  end
  if #args > 0 then
    local value = args[1]
    if tonumber(value) then
      value = tonumber(value) > 0
    else
      value = ({["true"]=true,["on"]=true,["yes"]=true})[value] ~= nil
    end
    rs.setWirelessOutput(value)
  end
  io.write("in: " .. tostring(rs.getWirelessInput()) .. "\n")
  io.write("out: " .. tostring(rs.getWirelessOutput()) .. "\n")
elseif options.f then
  if not rs.setWirelessOutput then
    io.stderr:write("wireless redstone not available\n")
    return 1
  end
  if #args > 0 then
    local value = args[1]
    if not tonumber(value) then
      io.stderr:write("invalid frequency\n")
      return 1
    end
    rs.setWirelessFrequency(tonumber(value))
  end
  io.write("freq: " .. tostring(rs.getWirelessFrequency()) .. "\n")
else
  local side = sides[args[1]]
  if not side then
    io.stderr:write("invalid side\n")
    return 1
  end
  if type(side) == "string" then
    side = sides[side]
  end

  if options.b then
    if not rs.setBundledOutput then
      io.stderr:write("bundled redstone not available\n")
      return 1
    end
    local color = colors[args[2]]
    if not color then
      io.stderr:write("invalid color\n")
      return 1
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
    io.write("in: " .. rs.getBundledInput(side, color) .. "\n")
    io.write("out: " .. rs.getBundledOutput(side, color) .. "\n")
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
    io.write("in: " .. rs.getInput(side) .. "\n")
    io.write("out: " .. rs.getOutput(side) .. "\n")
  end
end
