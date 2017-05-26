local unicode = require("unicode")
local event = require("event")
local kb = require("keyboard")
local component = require("component")
local computer = require("computer")
local keys = kb.keys

local tty = {}
tty.window =
{
  fullscreen = true,
  blink = true,
  dx = 0,
  dy = 0,
  x = 1,
  y = 1,
}

tty.internal = {}

local function ctrl_movement(cursor, dir)
  local index, data = cursor.index, cursor.data

  local last=dir<0 and 0 or unicode.len(data)
  local start=index+dir+1
  for i=start,last,dir do
    local a,b = unicode.sub(data, i-1, i-1), unicode.sub(data, i, i)
    a = a == "" or not not a:find("%s")
    b = b == "" or not not b:find("%s")
    if a and not b then return i - (index + 1) end
  end
  return last - index
end

local function read_history(handler, cursor, change)
  local ni = handler.index + change
  if ni >= 0 and ni <= #handler then
    handler[handler.index] = cursor.data
    handler.index = ni
    cursor:clear()
    cursor:update(handler[ni])
  end
end

local function tab_handler(handler, cursor)
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

local function key_down_handler(handler, cursor, char, code)
  local c = false
  local backup_cache = handler.cache
  handler.cache = nil
  local ctrl = kb.isControlDown(tty.keyboard())
  if ctrl and code == keys.d then
    return --close
  elseif code == keys.tab then
    handler.cache = backup_cache
    tab_handler(handler, cursor)
  elseif code == keys.enter or code == keys.numpadenter then
    cursor:move(math.huge)
    cursor:draw("\n")
    if #cursor.data > 0 then
      table.insert(handler, 1, cursor.data)
      handler[(tonumber(os.getenv("HISTSIZE")) or 10)+1]=nil
      handler[0]=nil
    end
    return nil, cursor.data .. "\n"
  elseif code == keys.up     then read_history(handler, cursor,  1)
  elseif code == keys.down   then read_history(handler, cursor, -1)
  elseif code == keys.left   then cursor:move(ctrl and ctrl_movement(cursor, -1) or -1)
  elseif code == keys.right  then cursor:move(ctrl and ctrl_movement(cursor,  1) or  1)
  elseif code == keys.home   then cursor:move(-math.huge)
  elseif code == keys["end"] then cursor:move( math.huge)
  elseif code == keys.back   then c = -1
  elseif code == keys.delete then c =  1
  --elseif ctrl and char == "w"then -- TODO: cut word
  elseif char >= 32          then c = unicode.char(char)
  else                            handler.cache = backup_cache -- ignored chars shouldn't clear hint cache
  end
  return c
end

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
  tty.scroll(math.huge)
  tty.setCursor(1, 1)
end

function tty.isAvailable()
  local gpu = tty.gpu()
  return not not (gpu and gpu.getScreen())
end

function tty.pull(cursor, timeout, ...)
  timeout = timeout or math.huge

  local width, height, dx, dy, x, y = tty.getViewport()
  local out = (x<1 or x>width or y<1 or y>height)
  local blink = tty.getCursorBlink()

  if cursor and out then
    cursor:move(0)
    cursor:scroll()
    out = false
  end

  x, y = tty.getCursor()
  x, y = x + dx, y + dy
  local gpu = not out and tty.gpu()

  local bgColor, bgIsPalette
  local fgColor, fgIsPalette
  local char_at_cursor
  if gpu then
    bgColor, bgIsPalette = gpu.getBackground()
    -- it can happen during a type of race condition when a screen is removed
    if not bgColor then
      return nil, "interrupted"
    end

    fgColor, fgIsPalette = gpu.getForeground()
    char_at_cursor = gpu.get(x, y)
  end

  -- get the next event
  local blinked = false
  local done = false
  local signal
  while true do
    if gpu then
      if not blinked and not done then
        gpu.setForeground(bgColor, bgIsPalette)
        gpu.setBackground(fgColor, fgIsPalette)
        gpu.set(x, y, char_at_cursor)
        gpu.setForeground(fgColor, fgIsPalette)
        gpu.setBackground(bgColor, bgIsPalette)
        blinked = true
      elseif blinked and (done or blink) then
        gpu.set(x, y, char_at_cursor)
        blinked = false
      end
    end

    if done then
      return table.unpack(signal, 1, signal.n)
    end

    signal = table.pack(event.pull(math.min(.5, timeout), ...))
    timeout = timeout - .5
    done = signal.n > 1 or timeout < .5
  end
end

function tty.internal.split(cursor)
  local data, index = cursor.data, cursor.index
  local dlen = unicode.len(data)
  index = math.max(0, math.min(index, dlen))
  local tail = dlen - index
  return unicode.sub(data, 1, index), tail == 0 and "" or unicode.sub(data, -tail)
end

function tty.internal.build_vertical_reader()
  local x, y = tty.getCursor()
  return
  {
    promptx = x,
    prompty = y,
    index = 0,
    data = "",
    sy = 0,
    scroll = function(_)
      _.sy = _.sy + tty.scroll()
    end,
    move = function(_,n)
      local win = tty.window
      _.index = math.min(math.max(0,_.index + n), unicode.len(_.data))
      local s1, s2 = tty.internal.split(_)
      s2 = unicode.sub(s2.." ", 1, 1)
      local data_remaining = ("_"):rep(_.promptx - 1)..s1..s2
      win.y = _.prompty - _.sy
      while true do
        local wlen_remaining = unicode.wlen(data_remaining)
        if wlen_remaining > win.width then
          local line_cut = unicode.wtrunc(data_remaining, win.width + 1)
          data_remaining = unicode.sub(data_remaining, unicode.len(line_cut) + 1)
          win.y = win.y + 1
        else
          win.x = wlen_remaining - unicode.wlen(s2) + 1
          break
        end
      end
    end,
    clear_tail = function(_)
      local oi, width, height, dx, dy, ox, oy = _.index, tty.getViewport()
      _:move(math.huge)
      _:move(-1)
      local ex, ey = tty.getCursor()
      tty.setCursor(ox, oy)
      _.index = oi
      local x = oy == ey and ox or 1
      tty.gpu().fill(x + dx, ey + dy, width - x + 1, 1, " ")
    end,
    update = function(_, arg)
      local s1, s2 = tty.internal.split(_)
      if type(arg) == "string" then
        _.data = s1 .. arg .. s2
        _.index = _.index + unicode.len(arg)
        _:draw(arg)
      else -- number
        if arg < 0 then
          -- backspace? ignore if at start
          if _.index <= 0 then return end
          _:move(arg)
          s1 = unicode.sub(s1, 1, -1 + arg)
        else
          -- forward? ignore if at end
          if _.index >= unicode.len(_.data) then return end
          s2 = unicode.sub(s2, 1 + arg)
        end
        _:clear_tail()
        _.data = s1 .. s2
      end

      -- redraw suffix
      if s2 ~= "" then
        local ps, px, py = _.sy, tty.getCursor()
        _:draw(s2)
        tty.setCursor(px, py - (_.sy - ps))
      end
    end,
    clear = function(_)
      _:move(-math.huge)
      _:draw((" "):rep(unicode.wlen(_.data)))
      _:move(-math.huge)
      _.index = 0
      _.data = ""
    end,
    draw = function(_, text)
      _.sy = _.sy + tty.drawText(text)
    end
  }
end

function tty.read(handler, cursor)
  if not io.stdin.tty then return io.read() end

  checkArg(1, handler, "table")
  checkArg(2, cursor, "table", "nil")

  handler.index = 0
  handler.touch = handler.touch or "touch_handler"
  handler.drag = handler.drag or "touch_handler"
  handler.clipboard = handler.clipboard or "clipboard_handler"
  handler.key_down = handler.key_down or key_down_handler

  cursor = cursor or tty.internal.build_vertical_reader()

  while true do
    local name, address, char, code = tty.pull(cursor)
    -- we may have lost tty during the pull
    if not tty.isAvailable() then
      return
    end

    -- we have to keep checking what kb is active in case it is switching during use
    -- we could have multiple screens, each with keyboards active
    local main_kb = tty.keyboard()
    local main_sc = tty.screen()
    if name == "interrupted" then
      tty.drawText("^C\n")
      return false
    elseif address == main_kb or address == main_sc then
      local handler_method = handler[name]
      if handler_method then
        if type(handler_method) == "string" then -- special hack to delay loading tty stuff
          handler_method = tty[handler_method]
        end
        -- nil to end (close)
        -- false to ignore
        -- true-thy updates cursor
        local c, ret = handler_method(handler, cursor, char, code)
        if c == nil then
          return ret
        elseif c then
        -- if we obtained something (c) to handle
          cursor:update(c)
        end
      end
    end
  end
end

-- cannot use tty.write = io.write because io.write invokes metatable
function tty.write(value, wrap)
  local stdout = io.output()
  local stream = stdout and stdout.stream
  local previous_nowrap = stream.nowrap
  stream.nowrap = wrap == false
  stdout:write(value)
  stdout:flush()
  stream.nowrap = previous_nowrap
end

function tty.getCursor()
  local window = tty.window
  return window.x, window.y
end

function tty.setCursor(x, y)
  local window = tty.window
  window.x, window.y = x, y
end

function tty.drawText(value, nowrap)
  local gpu = tty.gpu()
  if not gpu then
    return
  end
  local sy = 0
  local cr_last, beeped
  local uptime = computer.uptime
  local last_sleep = uptime()
  local last_index = 1
  local width, height, dx, dy = tty.getViewport()
  while true do
    if uptime() - last_sleep > 1 then
      os.sleep(0)
      last_sleep = uptime()
    end

    -- scroll before parsing next line
    -- the value may only have been a newline
    sy = sy + tty.scroll()
    local x, y = tty.getCursor()

    local si, ei, segment, delim = value:find("([^\t\r\n\a]*)([\t\r\n\a]?)", last_index)
    if si > ei then
      break
    end
    last_index = ei + 1

    if segment ~= "" then
      local gpu_x, gpu_y = x + dx, y + dy
      local tail = ""
      local wlen_needed = unicode.wlen(segment)
      local wlen_remaining = width - x + 1
      if wlen_remaining < wlen_needed then
        segment = unicode.wtrunc(segment, wlen_remaining + 1)
        wlen_needed = unicode.wlen(segment)
        -- we can clear the line because we already know remaining < needed
        tail = (" "):rep(wlen_remaining - wlen_needed)
        -- we have to reparse the delimeter
        last_index = si + #segment
        -- fake a newline
        if not nowrap then
          delim = "\n" 
        end
      end
      gpu.set(gpu_x, gpu_y, segment..tail)
      x = x + wlen_needed
    end

    if delim == "\t" then
      x = ((x-1) - ((x-1) % 8)) + 9
    elseif delim == "\r" or (delim == "\n" and not cr_last) then
      x = 1
      y = y + 1
    elseif delim == "\a" and not beeped then
      computer.beep()
      beeped = true
    end

    tty.setCursor(x, y)
    cr_last = delim == "\r"
  end
  return sy
end

function tty.setCursorBlink(enabled)
  tty.window.blink = enabled
end

function tty.getCursorBlink()
  return tty.window.blink
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
  window.gpu = gpu
  window.keyboard = nil -- without a keyboard bound, always use the screen's main keyboard (1st)
  screen_reset(gpu)
  tty.getViewport()
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

  -- if we are using a gpu bound to the primary scren, then use the primary keyboard
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

function tty.scroll(number)
  local gpu = tty.gpu()
  if not gpu then
    return 0
  end
  local width, height, dx, dy, x, y = tty.getViewport()

  local lines = number or (y - height)
  if lines == 0 -- if zero scroll length is requested, do nothing
     or not number and lines < 0 then -- do not auto scroll back up, only down
    return 0 
  end

  lines = math.min(lines, height)
  lines = math.max(lines,-height)

  -- scroll request can be too large
  local abs_lines = math.abs(lines)
  local box_height = height - abs_lines
  local fill_top = dy + 1 + (lines < 0 and 0 or box_height)

  gpu.copy(dx + 1, dy + 1 + math.max(0, lines), width, box_height, 0, -lines)
  gpu.fill(dx + 1, fill_top, width, abs_lines, ' ')

  tty.setCursor(x, math.min(y, height))

  return lines
end

setmetatable(tty, 
{
  __index = function(tbl, key)
    setmetatable(tbl, nil)
    dofile("/opt/core/full_tty.lua")
    return rawget(tbl, key)
  end
})

return tty
