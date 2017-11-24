local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term") -- using term for negative scroll feature
local text = require("text")
local unicode = require("unicode")
local computer = require("computer")
local tx = require("transforms")

if not io.output().tty then
  return loadfile(shell.resolve("cat", "lua"), "bt", _G)(...)
end

local args = shell.parse(...)
if #args > 1 then
  io.write("Usage: more <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end
local arg = args[1] or "-"

local initial_offset

-- test validity of args
do
  if arg == "-" then
    if not io.stdin then
      io.stderr:write("this process has no stdin\n")
      return 1
    end
    -- stdin may not be core_stdin
    initial_offset = io.stdin:seek("cur")
  else
    local file, reason = io.open(shell.resolve(arg))
    if not file then
      io.stderr:write(reason,'\n')
      return 1
    end
    initial_offset = file:seek("cur")
    file:close()
  end
end

local width, height = term.getViewport()
local max_display = height - 1

-- mgr is the data manager, it keeps track of what has been loaded
-- keeps a reasonable buffer, and keeps track of file handles
local mgr
mgr =
{
  lines = {}, -- current buffer
  chunk, -- temp from last read line that hasn't finished wrapping
  lines_released = 0,
  can_seek = initial_offset,
  capacity = math.max(1, math.min(max_display * 10, computer.freeMemory() / 2 / width)),
  size = 0,
  file = nil,
  path = arg ~= "-" and shell.resolve(arg) or nil,
  open = function()
    mgr.file = mgr.path and io.open(mgr.path) or io.stdin
  end,
  top_of_file = max_display,
  total_lines = nil, -- nil means unknown
  latest_line = nil, -- used for status improvements
  rollback = function()
    if not mgr.can_seek then
      return false
    end
    if not mgr.file then
      mgr.open()
    elseif not mgr.file:seek("set", 0) then
      mgr.close()
      return false
    end
    mgr.lines_released = 0
    mgr.lines = {}
    mgr.size = 0
    return true
  end,
  at = function(line_number)
    local index = line_number - mgr.lines_released
    if index < 1 then
      index = index + mgr.capacity
      if #mgr.lines ~= mgr.capacity or index <= mgr.size then
        return nil
      end
    elseif index > mgr.size then
      return nil
    end
    return mgr.lines[index] -- cached
  end,
  load = function(line_number)
    local index = line_number - mgr.lines_released
    if mgr.total_lines and mgr.total_lines < line_number then
      return nil
    end
    if mgr.at(line_number) then
      return true
    end
    -- lines[index] is line (lines_released + index) in the file
    -- thus index == line_number - lines_released
    if index <= 0 then
      -- we have previously freed some of the buffer, and now the user wants it back
      if not mgr.rollback() then
        -- TODO how to nicely fail if can_seek == false
        -- or if no more buffers
        error("cannot load prior data")
      end
      return mgr.load(line_number) -- retry
    end
    if mgr.read_next() then
      return mgr.load(line_number) -- retry
    end
    -- ran out of file, could not reach line_number
  end,
  write = function(line_number)
    local line = mgr.at(line_number)
    if not line then return false end
    term.write(line)
  end,
  close = function()
    if mgr.file then
      mgr.file:close()
      mgr.file = nil
    end
  end,
  last = function()
    -- return the last line_number available right now in the cache
    return mgr.size + mgr.lines_released
  end,
  check_capacity = function(release)
    -- if we have reached capacity
    if mgr.size >= mgr.capacity then
      if release then
        mgr.lines_released = mgr.lines_released + mgr.size
        mgr.size = 0
      end
      return true
    end
  end,
  insert = function(line)
    if mgr.check_capacity() then return false end
    mgr.size = mgr.size + 1
    mgr.lines[mgr.size] = line
    -- latest_line is not used for computation, just for status reports
    mgr.latest_line = math.max(mgr.latest_line or 0, mgr.size + mgr.lines_released)
    return true
  end,
  read_next = function()
    -- total_lines indicates we've reached the end previously
    -- but have we just prior to this reached the end?
    if mgr.last() == mgr.total_lines then
      -- then there is no more after that point
      return nil
    end
    if not mgr.file then
      mgr.open()
    end
    mgr.check_capacity(true)
    if not mgr.chunk then
      mgr.chunk = mgr.file:read("*l")
      if not mgr.chunk then
        mgr.total_lines = mgr.size + mgr.lines_released -- now file length is known
        mgr.close()
      end
    end
    while mgr.chunk do
      local wrapped, next = text.wrap(text.detab(mgr.chunk), width, width)
      -- insert fails if capacity is full
      if not mgr.insert(wrapped) then
        return mgr.last()
      end
      mgr.chunk = next
    end

    return mgr.last()
  end,
  scroll = function(num)
    if num < 0 then
      num = math.max(num, mgr.top_of_file)
      if num >= 0 then
        return true -- nothing to scroll
      end
    end

    term.setCursor(1, height)
    local y = height
    term.clearLine()

    if num < 0 then
      term.scroll(num) -- push text down
      mgr.top_of_file = mgr.top_of_file - num
      y = 1
      term.setCursor(1, y) -- ready to write lines above
      num = -num -- now print forward
    end

    local range
    while num > 0 do
      -- trigger load of data if needed
      local line_number = y - mgr.top_of_file
      
      if not mgr.load(line_number) then -- nothing more to read from the file
        return range ~= nil -- first time it is nil
      end

      -- print num range of what is available, scroll to show it (if bottom of screen)
      range = math.min(num, mgr.last() - line_number + 1)

      if y == height then
        range = math.min(range, max_display)
        term.scroll(range)
        y = y - range
        term.setCursor(1, y)
        mgr.top_of_file = mgr.top_of_file - range
      end

      for i=1,range do
        mgr.write(line_number + i - 1)
        term.setCursor(1, y + i)
      end
      y = y + range

      num = num - range
    end

    return true
  end,
  print_status = function()
    local first = mgr.top_of_file >= 1 and 1 or 1 - mgr.top_of_file
    local perc = not mgr.total_lines and "--" or tostring((max_display - mgr.top_of_file) / mgr.total_lines * 100):gsub("%..*","")
    local last_plus = mgr.total_lines and "" or "+"
    local status = string.format("%s lines %d-%d/%s %s%%", mgr.path or "-", first, max_display - mgr.top_of_file, tostring(mgr.total_lines or mgr.latest_line)..last_plus, perc)

    local gpu = term.gpu()
    local sf, sb = gpu.setForeground, gpu.setBackground
    local b_color, b_is_palette = gpu.getBackground()
    local f_color, f_is_palette = gpu.getForeground()
    sf(b_color, b_is_palette)
    sb(f_color, f_is_palette)
    term.write(status)
    sb(b_color, b_is_palette)
    sf(f_color, f_is_palette)
  end
}

local function update(num)
  -- unexpected
  if num == 0 then
    return
  end
  
  -- if this a positive direction, and we didn't previously know this was the end of the stream, give the user a once chance
  local end_is_known = mgr.total_lines
  -- clear buttom line, for status
  local ok = mgr.scroll(num or max_display)

  -- print status
  term.setCursor(1, height)
  -- we have to clear again in case we scrolled up
  term.clearLine()
  mgr.print_status()
  return not end_is_known or ok
end

if not update() then
  return
end

while true do
  local ename, address, char, code, dy = term.pull()
  local num
  if ename == "scroll" then
    if dy < 0 then
      num = 3
    else
      num = -3
    end
  elseif ename == "key_down" then
    num = 0
    if code == keyboard.keys.q or code == keyboard.keys.d and keyboard.isControlDown() then
      break
    elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
      num = max_display
    elseif code == keyboard.keys.pageUp then
      num = -max_display
    elseif code == keyboard.keys.enter or code == keyboard.keys.down then
      num = 1
    elseif code == keyboard.keys.up then
      num = -1
    elseif code == keyboard.keys.home then
      num = -math.huge
    elseif code == keyboard.keys["end"] then
      num = math.huge
    end
  elseif ename == "interrupted" then
    break
  end
  if num then
    update(num)
  end
end

term.clearLine()
