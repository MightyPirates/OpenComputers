local colors = {
  [0] = "white",
  [1] = "orange",
  [2] = "magenta",
  [3] = "lightblue",
  [4] = "yellow",
  [5] = "lime",
  [6] = "pink",
  [7] = "gray",
  [8] = "silver",
  [9] = "cyan",
  [10] = "purple",
  [11] = "blue",
  [12] = "brown",
  [13] = "green",
  [14] = "red",
  [15] = "black"
}

do
  local keys = {}
  for k in pairs(colors) do
    table.insert(keys, k)
  end
  for _, k in pairs(keys) do
    colors[colors[k]] = k
  end
end

return colors