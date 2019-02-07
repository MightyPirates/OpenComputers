local unicode = require("unicode")
local event = require("event")
local component = require("component")
local computer = require("computer")

local tty = {}
tty.window =
{
  fullscreen = true,
  blink = true,
  dx = 0,
  dy = 0,
  x = 1,
  y = 1,
  output_buffer = "",
}

tty.stream = {}

local screen_cache = {}
local function screen_reset(gpu, addr)
  screen_cache[addr or gpu.getScreen() or false] = nil
end

event.listen("screen_resized", screen_reset)

function tty.getViewport()
  local window = tty.window
  local screen = tty.screen()
  if window.fullscreen and screen and not screen_cache[screen] then
    screen_cache[screen] = true
    window.width, window.height = window.gpu.getViewport()
  end

  return window.width, window.height, window.dx, window.dy, window.x, window.y
end

function tty.setViewport(width, height, dx, dy, x, y)
  local window = tty.window
  dx, dy, x, y = dx or 0, dy or 0, x or 1, y or 1
  window.width, window.height, window.dx, window.dy, window.x, window.y = width, height, dx, dy, x, y
end

function tty.gpu()
  return tty.window.gpu
end

function tty.clear()
  tty.stream.scroll(math.huge)
  tty.setCursor(1, 1)
end

function tty.isAvailable()
  local gpu = tty.gpu()
  return not not (gpu and gpu.getScreen())
end

-- PLEASE do not use this method directly, use io.read or term.read
function tty.stream.read()
  local core = require("core/cursor")
  local cursor = core.new(tty.window.cursor)
  -- the window is given the cursor to allow sy updates [needed for wide char wrapping]
  -- even if the user didn't set a cursor, we need one to read
  tty.window.cursor = cursor

  local ok, result, reason = xpcall(core.read, debug.traceback, cursor)

  if not ok or not result then
    pcall(cursor.update, cursor)
  end

  return select(2, assert(ok, result, reason))
end

-- PLEASE do not use this method directly, use io.write or term.write
function tty.stream:write(value)
  local gpu = tty.gpu()
  if not gpu then
    return
  end
  local window = tty.window
  local cursor = window.cursor or {}
  cursor.sy = cursor.sy or 0
  cursor.tails = cursor.tails or {}
  local beeped
  local uptime = computer.uptime
  local last_sleep = uptime()
  window.output_buffer = window.output_buffer .. value
  while true do
    if uptime() - last_sleep > 3 then
      os.sleep(0)
      last_sleep = uptime()
    end

    local ansi_print = require("vt100").parse(window)

    -- scroll before parsing next line
    -- the value may only have been a newline
    cursor.sy = cursor.sy + self.scroll()
    -- we may have needed to scroll one last time [nowrap adjustments]
    -- or the vt100 parse is incomplete, print nothing else
    if #window.output_buffer == 0 or not ansi_print then
      break
    end

    local x, y = tty.getCursor()

    local _, ei, delim = unicode.sub(window.output_buffer, 1, window.width):find("([\27\t\r\n\a\b\v\15])")
    local segment = ansi_print .. (ei and window.output_buffer:sub(1, ei - 1) or window.output_buffer)

    if segment ~= "" then
      local gpu_x, gpu_y = x + window.dx, y + window.dy
      local tail = ""
      local wlen_needed = unicode.wlen(segment)
      local wlen_remaining = window.width - x + 1
      if wlen_remaining < wlen_needed then
        segment = unicode.wtrunc(segment, wlen_remaining + 1)
        wlen_needed = unicode.wlen(segment)
        tail = wlen_needed < wlen_remaining and " " or ""
        cursor.tails[gpu_y - cursor.sy] = tail
        if not window.nowrap then
          -- we have to reparse the delimeter
          ei = #segment
          -- fake a newline
          delim = "\n"
        end
      end
      gpu.set(gpu_x, gpu_y, segment..tail)
      x = x + wlen_needed
    end

    window.output_buffer = ei and window.output_buffer:sub(ei + 1) or
      unicode.sub(window.output_buffer, window.width + 1)

    if delim == "\t" then
      x = ((x-1) - ((x-1) % 8)) + 9
    elseif delim == "\r" then
      x = 1
    elseif delim == "\n" then
      x = 1
      y = y + 1
    elseif delim == "\b" then
      x = x - 1
    elseif delim == "\v" then
      y = y + 1
    elseif delim == "\a" and not beeped then
      computer.beep()
      beeped = true
    elseif delim == "\27" then
      window.output_buffer = delim .. window.output_buffer
    end

    tty.setCursor(x, y)
  end
  return cursor.sy
end

function tty.getCursor()
  local window = tty.window
  return window.x, window.y
end

function tty.setCursor(x, y)
  checkArg(1, x, "number")
  checkArg(2, y, "number")
  local window = tty.window
  window.x, window.y = x, y
end

local gpu_intercept = {}
function tty.bind(gpu)
  checkArg(1, gpu, "table")
  if not gpu_intercept[gpu] then
    gpu_intercept[gpu] = true -- only override a gpu once
    -- the gpu can change resolution before we get a chance to call events and handle screen_resized
    -- unfortunately, we have to handle viewport changes by intercept
    local setr, setv = gpu.setResolution, gpu.setViewport
    gpu.setResolution = function(...)
      screen_reset(gpu)
      return setr(...)
    end
    gpu.setViewport = function(...)
      screen_reset(gpu)
      return setv(...)
    end
  end
  local window = tty.window
  if window.gpu ~= gpu then
    window.gpu = gpu
    window.keyboard = nil -- without a keyboard bound, always use the screen's main keyboard (1st)
    tty.getViewport()
  end
  screen_reset(gpu)
end

function tty.keyboard()
  -- this method needs to be safe even if there is no terminal window (e.g. no gpu)
  local window = tty.window

  if window.keyboard then
    return window.keyboard
  end

  local system_keyboard = component.isAvailable("keyboard") and component.keyboard
  system_keyboard = system_keyboard and system_keyboard.address or "no_system_keyboard"

  local screen = tty.screen()

  if not screen then
    -- no screen, no known keyboard, use system primary keyboard if any
    return system_keyboard
  end

  -- if we are using a gpu bound to the primary screen, then use the primary keyboard
  if component.isAvailable("screen") and component.screen.address == screen then
    window.keyboard = system_keyboard
  else
    -- calling getKeyboards() on the screen is costly (time)
    -- changes to this design should avoid this on every key hit

    -- this is expensive (slow!)
    window.keyboard = component.invoke(screen, "getKeyboards")[1] or system_keyboard
  end

  return window.keyboard
end

function tty.screen()
  local gpu = tty.gpu()
  if not gpu then
    return nil
  end
  return gpu.getScreen()
end

function tty.stream.scroll(lines)
  local gpu = tty.gpu()
  if not gpu then
    return 0
  end
  local width, height, dx, dy, x, y = tty.getViewport()

  -- nil lines indicates a request to auto scroll
  -- auto scroll is when the cursor has gone below the bottom on the terminal
  -- and the text is scroll up, pulling the cursor back into view

  -- lines<0 scrolls up (text down)
  -- lines>0 scrolls down (text up)

  -- no lines count given, the user is asking to auto scroll y back into view
  if not lines then
    if y < 1 then
      lines = y - 1 -- y==0 scrolls back -1
    elseif y > height then
      lines = y - height -- y==height+1 scroll forward 1
    else
      return 0 -- do nothing
    end
  end

  lines = math.min(lines, height)
  lines = math.max(lines,-height)

  -- scroll request can be too large
  local abs_lines = math.abs(lines)
  local box_height = height - abs_lines
  local fill_top = dy + 1 + (lines < 0 and 0 or box_height)

  gpu.copy(dx + 1, dy + 1 + math.max(0, lines), width, box_height, 0, -lines)
  gpu.fill(dx + 1, fill_top, width, abs_lines, ' ')

  tty.setCursor(x, math.max(1, math.min(y, height)))
  return lines
end

-- stream methods
local function bfd() return nil, "tty: invalid operation" end
tty.stream.close = bfd
tty.stream.seek = bfd
tty.stream.handle = "tty"

return tty
