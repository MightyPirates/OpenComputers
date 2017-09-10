local text = require("text")
local vt100 = {}

-- runs patterns on ansi until failure
-- returns valid:boolean, completed_index:nil|number
function vt100.validate(ansi, patterns)
  local last_index = 0
  local captures = {}
  for _,pattern in ipairs(patterns) do
    if last_index >= #ansi then
      return true
    end
    local si, ei, capture = ansi:find("^(" .. pattern .. ")", last_index + 1)
    if not si then -- failed to match
      return
    end
    captures[#captures + 1] = capture
    last_index = ei
  end
  return true, last_index, captures
end

local rules = {}

-- colors
-- [%d+;%d+;..%d+m
rules[{"%[", "[%d;]*", "m"}] = function(_, _, number_text)
  local numbers = {}
  -- prefix and suffix ; act as reset
  -- e.g. \27[41;m is actually 41 followed by a reset
  number_text = ";" .. number_text:gsub("^;$","") .. ";"
  local parts = text.split(number_text, {";"})
  local last_was_break
  for _,part in ipairs(parts) do
    local num = tonumber(part)
    if not num then
      num = last_was_break and 0
      last_was_break = true
    else
      last_was_break = false
    end
    if num == 0 then
      numbers[#numbers + 1] = 40
      numbers[#numbers + 1] = 37
    elseif num then
      numbers[#numbers + 1] = num
    end
  end
  return numbers
end

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
-- [(%d+)A        move cursor up n lines
-- [(%d+)B        move cursor down n lines
-- [(%d+)C        move cursor right n lines
-- [(%d+)D        move cursor left n lines
rules[{"%[", "%d+", "[ABCD]"}] = function(window, _, n, dir)
  local dx, dy = 0, 0
  n = tonumber(n)
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
rules[{"%[", "%d+", ";", "%d+", "[Hf]"}] = function(window, _, y, _, x)
  set_cursor(window, tonumber(x), tonumber(y))
end

-- [K             clear line from cursor right
-- [0K            ^ same
-- [1K            clear line from cursor left
-- [2K            clear entire line
local function clear_line(window, _, n)
  n = tonumber(n) or 0
  local x = n == 0 and window.x or 1
  local rep = n == 1 and window.x or window.width
  window.gpu.set(x, window.y, (" "):rep(rep))
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
  local rep = n == 1 and (window.y - 1) or window.height
  window.gpu.fill(1, y, window.width, rep, " ")
end

-- [H             move cursor to upper left corner
-- [;H            ^ same
-- [f             ^ same
-- [;f            ^ same
rules[{"%[;?", "[Hf]"}] = function(window)
  set_cursor(window, 1, 1)
end

-- [6n            get the cursor position [ EscLine;ColumnR 	Response: cursor is at v,h ]
rules[{"%[", "6", "n"}] = function(window)
  -- this solution puts the response on stdin, but it isn't echo'd
  -- I'm personally fine with the lack of echo
  io.stdin.bufferRead = string.format("%s%s%d;%dR", io.stdin.bufferRead, string.char(0x1b), window.y, window.x)
end

-- D               scroll up one line -- moves cursor down
-- E               move to next line (acts the same ^, but x=1)
-- M               scroll down one line -- moves cursor up
rules[{"[DEM]"}] = function(window, dir)
  if dir == "D" then
    window.y = window.y + 1
  elseif dir == "E" then
    window.y = window.y + 1
    window.x = 1
  else -- M
    window.y = window.y - 1
  end
end

local function save_attributes(window, save)
  if save then
    local data = window.saved or {1, 1, {0x0}, {0xffffff}}
    window.x = data[1]
    window.y = data[2]
    window.gpu.setBackground(table.unpack(data[3]))
    window.gpu.setForeground(table.unpack(data[4]))
  else
    window.saved = {window.x, window.y, {window.gpu.getBackground()}, {window.gpu.getForeground()}}
  end
end

-- 7               save cursor position and attributes
-- 8               restore cursor position and attributes
rules[{"[78]"}] = function(window, restore)
  save_attributes(window, restore == "8")
end

-- s               save cursor position
-- u               restore cursor position
rules[{"%[", "[su]"}] = function(window, _, restore)
  save_attributes(window, restore == "u")
end

function vt100.parse(window)
  local ansi = window.ansi_escape
  window.ansi_escape = nil
  local any_valid

  for rule,action in pairs(rules) do
    local ok, completed, captures = vt100.validate(ansi, rule)
    if completed then
      return action(window, table.unpack(captures)) or {}, "", ansi:sub(completed + 1)
    elseif ok then
      any_valid = true
    end
  end

  if not any_valid then
    -- malformed
    return {}, string.char(0x1b), ansi
  end

  -- else, still consuming
  window.ansi_escape = ansi
  return {}, "", ""
end

return vt100
