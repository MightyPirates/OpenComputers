local tty = require("tty")
local unicode = require("unicode")

function tty.touch_handler(handler, cursor, gx, gy)
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

function tty.clipboard_handler(handler, cursor, char, code)
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

