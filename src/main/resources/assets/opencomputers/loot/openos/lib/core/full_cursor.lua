local core_cursor = require("core/cursor")
local unicode = require("unicode")
local kb = require("keyboard")

function core_cursor.touch(cursor, gx, gy)
  if cursor.data == "" then
    return true
  end
  local win = cursor.window
  gx, gy = gx - win.dx, gy - win.dy
  while true do
    local x, y, d = win.x, win.y, win.width
    local dx = ((gy*d+gx)-(y*d+x))
    if dx == 1 then
      dx = unicode.wlen(unicode.sub(cursor.data, cursor.index + 1, cursor.index + 1)) == 2 and 0 or dx
    end
    if dx == 0 then
      break
    end
    cursor:move(dx > 0 and 1 or -1)
    if x == win.x and y == win.y then
      break
    end
  end
  return true
end

function core_cursor.clipboard(cursor, char)
  cursor.cache = nil
  local first_line, end_index = char:find("\13?\10")
  local after = ""
  if first_line then
    after = char:sub(end_index + 1)
    char = char:sub(1, first_line - 1)
  end
  cursor:update(char)
  if after ~= "" then
    -- todo look at postponing the text on cursor
    local keyboard = require("tty").keyboard()
    require("computer").pushSignal("key_down", keyboard, 13, 28)
    require("computer").pushSignal("clipboard", keyboard, after)
  end
  return first_line and char or true
end

function core_cursor.tab(cursor)
  local hints = cursor.hint
  if not hints then return end
  if not cursor.cache then
    cursor.cache =
      type(hints) == "table" and hints or
      hints(cursor.data, cursor.index + 1) or
      {}
    cursor.cache.i = -1
  end

  local cache = cursor.cache
  
  if #cache == 1 and cache.i == 0 then
    -- there was only one solution, and the user is asking for the next
    cursor.cache = hints(cache[1], cursor.index + 1)
    if not cursor.cache then return end
    cursor.cache.i = -1
    cache = cursor.cache
  end

  local change = kb.isShiftDown() and -1 or 1
  cache.i = (cache.i + change) % math.max(#cache, 1)
  local next = cache[cache.i + 1]
  if next then
    local tail = unicode.len(cursor.data) - cursor.index
    cursor:move(cursor.len)
    cursor:update(-cursor.len)
    cursor:update(next, -tail)
  end
end
