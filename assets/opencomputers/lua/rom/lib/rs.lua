local function stringToSide(side)
  if type(side) == "string" and rs.sides[side] then
    return rs.sides[side]
  end
  return side
end

-------------------------------------------------------------------------------

rs = {}
redstone = rs

function rs.analogInput(side)
  return driver.redstone.analogInput(component.primary("redstone"), stringToSide(side))
end

function rs.analogOutput(side, value)
  return driver.redstone.analogOutput(component.primary("redstone"), stringToSide(side), value)
end

function rs.input(side)
  return driver.redstone.input(component.primary("redstone"), stringToSide(side))
end

function rs.output(side, value)
  return driver.redstone.output(component.primary("redstone"), stringToSide(side), value)
end

-------------------------------------------------------------------------------

rs.sides = {
  [0] = "bottom",
  [1] = "top",
  [2] = "back",
  [3] = "front",
  [4] = "right",
  [5] = "left"
}
for k, v in pairs(rs.sides) do
  rs.sides[v] = k
end
rs.sides.up = rs.sides.top
rs.sides.down = rs.sides.bottom
