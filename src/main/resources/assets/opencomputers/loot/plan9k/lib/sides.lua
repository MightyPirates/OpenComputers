local sides = {
  [0] = "bottom",
  [1] = "top",
  [2] = "back",
  [3] = "front",
  [4] = "right",
  [5] = "left",
  [6] = "unknown",

  bottom = 0,
  top = 1,
  back = 2,
  front = 3,
  right = 4,
  left = 5,
  unknown = 6,

  down = 0,
  up = 1,
  north = 2,
  south = 3,
  west = 4,
  east = 5,

  negy = 0,
  posy = 1,
  negz = 2,
  posz = 3,
  negx = 4,
  posx = 5,

  forward = 3
}

local metatable = getmetatable(sides) or {}

-- sides[0..5] are mapped to itertable[1..6].
local itertable = {
  sides[0],
  sides[1],
  sides[2],
  sides[3],
  sides[4],
  sides[5]
}

-- Future-proofing against the possible introduction of additional
-- logical sides (e.g. [7] = "all", [8] = "none", etc.).
function metatable.__len(sides)
  return #itertable
end

-- Allow `sides` to be iterated over like a normal (1-based) array.
function metatable.__ipairs(sides)
  return ipairs(itertable)
end

setmetatable(sides, metatable)

-------------------------------------------------------------------------------

return sides
