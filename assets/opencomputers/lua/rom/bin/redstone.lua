local args, options = shell.parse(...)
if #args < 1 then
  print("Usage: redstone <side> [<value>]")
  return
end

local rs = component.primary("redstone")

local side = sides[args[1]]
if not side then
  print("Invalid side.")
  return
end
if type(side) == "string" then
  side = sides[side]
end

if options.b then
  if not rs.setBundledOutput then
    print("Bundled redstone not available.")
    return
  end
  local color = colors[args[2]]
  if not color then
    print("Invalid color.")
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
