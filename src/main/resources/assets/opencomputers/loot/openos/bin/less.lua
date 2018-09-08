local keys = require("keyboard").keys
local shell = require("shell")
local unicode = require("unicode")
local term = require("term") -- using term for negative scroll feature

local args, ops = shell.parse(...)
if #args > 1 then
  io.write("Usage: ", os.getenv("_"):match("/([^/]+)%.lua$"), " <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end

local cat_cmd = table.concat({"cat", ...}, " ")
if not io.output().tty then
  return os.execute(cat_cmd)
end

local preader = io.popen(cat_cmd)
local scrollback = not ops.noback and {}
local bottom = 0
local end_of_buffer = false

local width, height = term.getViewport()

local function split(full_line)
  local index = 1
  local parts = {}
  while true do
    local sub = full_line:sub(index, index + width*3)
    -- checking #sub < width first is faster, save a unicode call
    if #sub < width or unicode.wlen(sub) <= width then
      parts[#parts + 1] = sub
      break
    end
    parts[#parts + 1] = unicode.wtrunc(sub, width + 1)
    index = index + #parts[#parts]
    if index > #full_line then
      break
    end
  end
  return parts
end

local function scan(num)
  local result = {}
  local line_count = 0
  for i=1, num do
    local lines = {}
    if scrollback and (bottom + i) <= #scrollback then
      lines = {scrollback[bottom + i]}
    else
      local full_line = preader:read()
      if not full_line then preader:close() break end
      -- with buffering, we can buffer ahead too, and read more smoothly
      local buffering = false
      for _,line in ipairs(split(full_line)) do
        if not buffering then
          lines[#lines + 1] = line
        end
        if scrollback then
          buffering = true
          scrollback[#scrollback + 1] = line
        end
      end
    end

    for _,line in ipairs(lines) do
      result[#result + 1] = line
      line_count = line_count + 1
      if #result > height then
        table.remove(result, 1)
      end
    end

    if line_count >= num then
      break
    end
  end
  return result, line_count
end

local function status()
  if end_of_buffer then
    if ops.noback then
      os.exit()
    end
    io.write("(END)")
  end
  io.write(":")
end

local function goback(n)
  if not scrollback then return end
  local current_top = bottom - height + 1
  n = math.min(current_top, n)
  if n < 1 then return end
  local top = current_top - n + 1
  term.scroll(-n)
  term.setCursor(1, 1)
  for i=1, n do
    if i >= height then
      break
    end
    print(scrollback[top + i - 1])
  end
  term.setCursor(1, height)
  bottom = bottom - n
  end_of_buffer = false
end

local function goforward(n)
  term.clearLine()
  local update, line_count = scan(n)
  for _,line in ipairs(update) do
    print(line)
  end
  if line_count < n then
    end_of_buffer = true
  end
  bottom = bottom + line_count
end

goforward(height - 1)

while true do
  term.clearLine()
  status()
  local e, _, _, code = term.pull()
  if e == "interrupted" then
    break
  elseif e == "key_down" then
    if code == keys.q then
      term.clearLine()
      os.exit() -- abort
    elseif code == keys["end"] then
      goforward(math.huge)
    elseif code == keys.space or code == keys.pageDown then
      goforward(height - 1)
    elseif code == keys.enter or code == keys.down then
      goforward(1)
    elseif code == keys.up then
      goback(1)
    elseif code == keys.pageUp then
      goback(height - 1)
    elseif code == keys.home then
      goback(math.huge)
    end
  end
end
