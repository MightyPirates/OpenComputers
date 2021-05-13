local vt100 = require("vt100")

local rules = vt100.rules

-- [?7[hl]        wrap mode
rules[{"%[", "%?", "7", "[hl]"}] = function(window, _, _, _, nowrap)
  window.nowrap = nowrap == "l"
end

-- helper scroll function
local function set_cursor(window, x, y)
  window.x = math.min(math.max(x, 1), window.width)
  window.y = math.min(math.max(y, 1), window.height)
end

-- -- These DO NOT SCROLL
-- [(%d*)A        move cursor up n lines
-- [(%d*)B        move cursor down n lines
-- [(%d*)C        move cursor right n lines
-- [(%d*)D        move cursor left n lines
rules[{"%[", "%d*", "[ABCD]"}] = function(window, _, n, dir)
  local dx, dy = 0, 0
  n = tonumber(n) or 1
  if dir == "A" then
    dy = -n
  elseif dir == "B" then
    dy = n
  elseif dir == "C" then
    dx = n
  else -- D
    dx = -n
  end
  set_cursor(window, window.x + dx, window.y + dy)
end

-- [Line;ColumnH  Move cursor to screen location v,h
-- [Line;Columnf  ^ same
-- [;H            Move cursor to upper left corner
-- [;f            ^ same
rules[{"%[", "%d*", ";", "%d*", "[Hf]"}] = function(window, _, y, _, x)
  set_cursor(window, tonumber(x) or 1, tonumber(y) or 1)
end

-- [H             move cursor to upper left corner
-- [f             ^ same
rules[{"%[[Hf]"}] = function(window)
  set_cursor(window, 1, 1)
end

-- [K             clear line from cursor right
-- [0K            ^ same
-- [1K            clear line from cursor left
-- [2K            clear entire line
local function clear_line(window, _, n)
  n = tonumber(n) or 0
  local x = (n == 0 and window.x or 1)
  local rep = n == 1 and window.x or (window.width - x + 1)
  window.gpu.fill(x + window.dx, window.y + window.dy, rep, 1, " ")
end
rules[{"%[", "[012]?", "K"}] = clear_line

-- [J             clear screen from cursor down
-- [0J            ^ same
-- [1J            clear screen from cursor up
-- [2J            clear entire screen
rules[{"%[", "[012]?", "J"}] = function(window, _, n)
  clear_line(window, _, n)
  n = tonumber(n) or 0
  local y = n == 0 and (window.y + 1) or 1
  local rep = n == 1 and (window.y - 1) or (window.height)
  window.gpu.fill(1 + window.dx, y + window.dy, window.width, rep, " ")
end

-- [6n            get the cursor position [ EscLine;ColumnR 	Response: cursor is at v,h ]
rules[{"%[", "6", "n"}] = function(window)
  -- this solution puts the response on stdin, but it isn't echo'd
  -- I'm personally fine with the lack of echo
  io.stdin.bufferRead = string.format("%s%s[%d;%dR", io.stdin.bufferRead, string.char(0x1b), window.y, window.x)
end

-- D               scroll up one line -- moves cursor down
-- E               move to next line (acts the same ^, but x=1)
-- M               scroll down one line -- moves cursor up
rules[{"[DEM]"}] = function(window, _, dir)
  if dir == "D" then
    window.y = window.y + 1
  elseif dir == "E" then
    window.y = window.y + 1
    window.x = 1
  else -- M
    window.y = window.y -  1
  end
end
