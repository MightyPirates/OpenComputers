rs = setmetatable({}, {__index=driver.redstone})

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