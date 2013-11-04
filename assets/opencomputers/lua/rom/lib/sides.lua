local sides = {
  [0] = "bottom",
  [1] = "top",
  [2] = "back",
  [3] = "front",
  [4] = "right",
  [5] = "left"
}
for k, v in pairs(sides) do
  sides[v] = k
end
sides.up = sides.top
sides.down = sides.bottom

-------------------------------------------------------------------------------

_G.sides = sides
