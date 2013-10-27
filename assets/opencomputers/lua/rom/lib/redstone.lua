local function stringToSide(side)
  if type(side) == "string" and rs.sides[side] then
    return rs.sides[side]
  end
  return side
end

-------------------------------------------------------------------------------

redstone = {}
rs = redstone

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

function rs.proxy(address)
  local proxy = component.proxy(address)
  local analogInput = proxy.analogInput
  local analogOutput = proxy.analogOutput
  function proxy.analogInput(side)
    return analogInput(stringToSide(side))
  end
  function proxy.analogOutput(side, value)
    return analogOutput(stringToSide(side), value)
  end
  function proxy.input(side)
    return proxy.analogInput(side) > 0
  end
  function proxy.output(side, value)
    if value then
      return proxy.analogOutput(side, value and 15 or 0)
    else
      return proxy.analogOutput(side) > 0
    end
  end
end
