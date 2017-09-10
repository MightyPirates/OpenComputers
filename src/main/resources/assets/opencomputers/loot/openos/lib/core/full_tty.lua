local tty = require("tty")
local unicode = require("unicode")
local kb = require("keyboard")

function tty.touch_handler(_, cursor, gx, gy)
  if cursor.data == "" then
    return false
  end
  cursor:move(-math.huge)
  local win = tty.window
  gx, gy = gx - win.dx, gy - win.dy
  local x2, y2, d = win.x, win.y, win.width
  local char_width_to_move = ((gy*d+gx)-(y2*d+x2))
  if char_width_to_move <= 0 then
    return false
  end
  local total_wlen = unicode.wlen(cursor.data)
  if char_width_to_move >= total_wlen then
    cursor:move(math.huge)
  else
    local chars_to_move = unicode.wtrunc(cursor.data, char_width_to_move + 1)
    cursor:move(unicode.len(chars_to_move))
  end
  -- fake white space can make the index off, redo adjustment for alignment
  x2, y2, d = win.x, win.y, win.width
  char_width_to_move = ((gy*d+gx)-(y2*d+x2))
  if (char_width_to_move < 0) then
    -- using char_width_to_move as a type of index is wrong, but large enough and helps to speed this up
    local up_to_cursor = unicode.sub(cursor.data, cursor.index+char_width_to_move, cursor.index)
    local full_wlen = unicode.wlen(up_to_cursor)
    local without_tail = unicode.wtrunc(up_to_cursor, full_wlen + char_width_to_move + 1)
    local chars_cut = unicode.len(up_to_cursor) - unicode.len(without_tail)
    cursor:move(-chars_cut)
  end
  return false -- no further cursor update
end
tty.drag_handler = tty.touch_handler

function tty.clipboard_handler(handler, _, char, _)
  handler.cache = nil
  local first_line, end_index = char:find("\13?\10")
  if first_line then
    local after = char:sub(end_index + 1)
    if after ~= "" then
      -- todo look at postponing the text on cursor
      require("computer").pushSignal("key_down", tty.keyboard(), 13, 28)
      require("computer").pushSignal("clipboard", tty.keyboard(), after)
    end
    char = char:sub(1, first_line - 1)
  end
  return char
end

function tty.on_tab(handler, cursor)
  local hints = handler.hint
  if not hints then return end
  local main_kb = tty.keyboard()
  -- tty may not have a keyboard
  -- in which case, we shouldn't be handling tab events
  if not main_kb then
    return
  end
  if not handler.cache then
    handler.cache = type(hints) == "table" and hints or hints(cursor.data, cursor.index + 1) or {}
    handler.cache.i = -1
  end

  local cache = handler.cache
  
  if #cache == 1 and cache.i == 0 then
    -- there was only one solution, and the user is asking for the next
    handler.cache = hints(cache[1], cursor.index + 1)
    if not handler.cache then return end
    handler.cache.i = -1
    cache = handler.cache
  end

  local change = kb.isShiftDown(main_kb) and -1 or 1
  cache.i = (cache.i + change) % math.max(#cache, 1)
  local next = cache[cache.i + 1]
  if next then
    local tail = unicode.len(cursor.data) - cursor.index
    cursor:clear()
    cursor:update(next)
    cursor:move(-tail)
  end
end
