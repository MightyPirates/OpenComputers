local rs = component.redstone
local args, options = shell.parse(...)
if #args < 1 then
  if rs.setBundledOutput then
    print("Usage: redstone <side> [-b <color>] [<value>]")
  else
    print("Usage: redstone <side> [<value>]")
  end
  return
end

local side = sides[args[1]]
if not side then
  error("Invalid side.")
end
if type(side) == "string" then
  side = sides[side]
end

if options.b then
  if not rs.setBundledOutput then
    error("Bundled redstone not available.")
  end
  local color = colors[args[2]]
  if not color then
    error("Invalid color.")
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
  print("in: " .. rs.getBundledInput(side, color))
  print("out: " .. rs.getBundledOutput(side, color))
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
  print("in: " .. rs.getInput(side))
  print("out: " .. rs.getOutput(side))
end
