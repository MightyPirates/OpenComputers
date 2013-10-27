local args = shell.parse(...)
if #args < 1 then
  print("Usage: redstone <side> [<value>]")
  return
end

local side = rs.sides[args[1]]
if not side then
  print("Invalid side.")
end
if type(side) == "string" then
  side = rs.sides[side]
end

if #args > 1 then
  local value = args[2]
  if tonumber(value) then
    value = tonumber(value)
  else
    value = ({["true"]=true,["on"]=true,["yes"]=true})[value] and 15 or 0
  end
  component.primary("redstone").setOutput(side, value)
else
  print("in: " .. component.primary("redstone").getInput(side))
  print("out: " .. component.primary("redstone").getOutput(side))
end
