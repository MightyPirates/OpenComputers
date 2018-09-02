local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term") -- using term for negative scroll feature

local args = shell.parse(...)
if #args > 1 then
  io.write("Usage: less <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end

local cat_cmd = table.concat({"cat", ...}, " ")
if not io.output().tty then
  return os.execute(cat_cmd)
end

term.clear()

local preader = io.popen(cat_cmd)
local buffer = setmetatable({}, {__index = function(tbl, index)
  if type(index) ~= "number" or index < 1 then return end
  while #tbl < index do
    local line = preader:read()
    if not line then return end
    table.insert(tbl, line)
  end
  return tbl[index]
end})

local index = 1

local _, height = term.getViewport()
local status = ":"

local function goback(n)
  n = math.min(index - height, n)
  if n <= 0 then return end -- make no back scroll, we're at the top
  term.scroll(-n)
  index = index - n
  for y = 1, math.min(height - 1, n) do
    term.setCursor(1, y)
    print(buffer[index - height + y])
  end
  term.setCursor(1, height)
end

local function goforward(n)
  if not buffer[index] then return end -- nothing to do
  for test_index = index, index + n do
    if not buffer[test_index] then
      n = math.min(n, test_index - index)
      break
    end
  end

  term.clearLine()
  term.scroll(n)
  if n >= height then
    index = index + (n - height) + 1
    n = height - 1
  end
  term.setCursor(1, height - n)

  return true
end

while true do
  local _, y = term.getCursor()
  local print_next_line = true
  if y == height then
    print_next_line = false
    term.clearLine()
    io.write(status)
    local _, _, _, code = term.pull("key_down")
    if code == keyboard.keys.q then
      term.clearLine()
      os.exit(1) -- abort
    elseif code == keyboard.keys["end"] then
      print_next_line = goforward(math.huge)
    elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
      print_next_line = goforward(height - 1)
    elseif code == keyboard.keys.enter or code == keyboard.keys.down then
      print_next_line = goforward(1)
    elseif code == keyboard.keys.up then
      goback(1)
    elseif code == keyboard.keys.pageUp then
      goback(height - 1)
    end
  end
  if print_next_line then
    local line = buffer[index]
    if line then
      print(line)
      index = index + 1
    else
      term.setCursor(1, height)
    end
  end
end
