local sides = {
  [0] = "bottom",
  [1] = "top",
  [2] = "back",
  [3] = "front",
  [4] = "right",
  [5] = "left",

  bottom = 0,
  top = 1,
  back = 2,
  front = 3,
  right = 4,
  left = 5
}

sides.up = sides.top
sides.down = sides.bottom

-------------------------------------------------------------------------------

_G.sides = sides
