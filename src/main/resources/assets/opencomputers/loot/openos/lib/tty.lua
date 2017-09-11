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

tty.stream = {}

function tty.key_down_handler(handler, cursor, char, code)
  local data = cursor.data
  local c = false
  local backup_cache = handler.cache
  handler.cache = nil
  local ctrl = kb.isControlDown(tty.keyboard())
  if ctrl and code == keys.d then
    return --close
  elseif code == keys.tab then
    handler.cache = backup_cache
    tty.on_tab(handler, cursor)
  elseif code == keys.enter or code == keys.numpadenter then
    cursor:move(math.huge)
    cursor:draw("\n")
    if #data > 0 then
      table.insert(handler, 1, data)
      handler[(tonumber(os.getenv("HISTSIZE")) or 10)+1]=nil
      handler[0]=nil
    end
    return nil, data .. "\n"
  elseif code == keys.up or code == keys.down then
    local ni = handler.index + (code == keys.up and 1 or -1)
    if ni >= 0 and ni <= #handler then
      handler[handler.index] = data
      handler.index = ni
      cursor:clear()
      cursor:update(handler[ni])
    end
  elseif code == keys.left or code == keys.back or code == keys.w and ctrl then
    local value = ctrl and ((unicode.sub(data, 1, cursor.index):find("%s[^%s]+%s*$") or 0) - cursor.index) or -1
    if code == keys.left then
      cursor:move(value)
    else
      c = value
    end
  elseif code == keys.right  then cursor:move(ctrl and ((data:find("%s[^%s]", cursor.index + 1) or math.huge) - cursor.index) or 1)
  elseif code == keys.home   then cursor:move(-math.huge)
  elseif code == keys["end"] then cursor:move( math.huge)
  elseif code == keys.delete then c =  1
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
  tty.stream.scroll(math.huge)
  tty.setCursor(1, 1)
end

function tty.isAvailable()
  local gpu = tty.gpu()
  return not not (gpu and gpu.getScreen())
end

function tty.stream:blink(done)
  local width, height, dx, dy, x, y = tty.getViewport()
  local gpu = tty.gpu()
  if not gpu or x < 1 or x > width or y < 1 or y > height then
    return true
  end
  x, y = x + dx, y + dy
  local blinked, bgColor, bgIsPalette, fgColor, fgIsPalette, char_at_cursor = table.unpack(self.blink_cache or {})
  if done == nil then -- reset
    blinked = false
    bgColor, bgIsPalette = gpu.getBackground()
    -- it can happen during a type of race condition when a screen is removed
    if not bgColor then
      return
    end

    fgColor, fgIsPalette = gpu.getForeground()
    char_at_cursor = gpu.get(x, y)
  end

  if not blinked and not done then
    gpu.setForeground(bgColor, bgIsPalette)
    gpu.setBackground(fgColor, fgIsPalette)
    gpu.set(x, y, char_at_cursor)
    gpu.setForeground(fgColor, fgIsPalette)
    gpu.setBackground(bgColor, bgIsPalette)
    blinked = true
  elseif blinked and (done or tty.window.blink) then
    gpu.set(x, y, char_at_cursor)
    blinked = false
  end

  self.blink_cache = table.pack(blinked, bgColor, bgIsPalette, fgColor, fgIsPalette, char_at_cursor)
  return true
end

function tty.stream:pull(timeout, ...)
  timeout = timeout or math.huge
  local blink_timeout = tty.window.blink and .5 or math.huge

  -- it can happen during a type of race condition when a screen is removed
  if not self:blink() then
    return nil, "interrupted"
  end

  -- get the next event
  while true do
    local signal = table.pack(event.pull(math.min(blink_timeout, timeout), ...))

    timeout = timeout - blink_timeout
    local done = signal.n > 1 or timeout < blink_timeout
    self:blink(done)

    if done then
      return table.unpack(signal, 1, signal.n)
    end
  end
end

function tty.split(cursor)
  local data, index = cursor.data, cursor.index
  local dlen = unicode.len(data)
  index = math.max(0, math.min(index, dlen))
  local tail = dlen - index
  return unicode.sub(data, 1, index), tail == 0 and "" or unicode.sub(data, -tail)
end

function tty.build_vertical_reader()
  return
  {
    promptx = tty.window.x,
    prompty = tty.window.y,
    index = 0,
    data = "",
    sy = 0,
    scroll = function(self, goback, prev_x, prev_y)
      local width, x = tty.window.width, tty.getCursor() - 1
      tty.setCursor(x % width + 1, tty.window.y + math.floor(x / width))
      self:draw("")
      if goback then
        tty.setCursor(prev_x, prev_y - self.sy)
      end
    end,
    move = function(self, n)
      local win = tty.window
      self.index = math.min(math.max(0, self.index + n), unicode.len(self.data))
      local s1, s2 = tty.split(self)
      s2 = unicode.sub(s2.." ", 1, 1)
      local data_remaining = ("_"):rep(self.promptx - 1)..s1..s2
      win.y = self.prompty - self.sy
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
    clear_tail = function(self)
      local oi, width, _, dx, dy, ox, oy = self.index, tty.getViewport()
      self:move(math.huge)
      self:move(-1)
      local _, ey = tty.getCursor()
      tty.setCursor(ox, oy)
      self.index = oi
      local cx = oy == ey and ox or 1
      tty.gpu().fill(cx + dx, ey + dy, width - cx + 1, 1, " ")
    end,
    update = function(self, arg)
      local s1, s2 = tty.split(self)
      if type(arg) == "string" then
        self.data = s1 .. arg .. s2
        self.index = self.index + unicode.len(arg)
        self:draw(arg)
      else -- number
        if arg < 0 then
          -- backspace? ignore if at start
          if self.index <= 0 then return end
          self:move(arg)
          s1 = unicode.sub(s1, 1, -1 + arg)
        else
          -- forward? ignore if at end
          if self.index >= unicode.len(self.data) then return end
          s2 = unicode.sub(s2, 1 + arg)
        end
        self:clear_tail()
        self.data = s1 .. s2
      end

      -- redraw suffix
      local prev_x, prev_y = tty.getCursor()
      prev_y = prev_y + self.sy -- scroll will remove it
      self:draw(s2)
      self:scroll(s2 ~= "", prev_x, prev_y)
    end,
    clear = function(self)
      self:move(-math.huge)
      self:draw((" "):rep(unicode.wlen(self.data)))
      self:move(-math.huge)
      self.index = 0
      self.data = ""
    end,
    draw = function(self, text)
      self.sy = self.sy + tty.stream:write(text)
    end
  }
end

function tty.read(handler)
  tty.window.handler = handler

  local stdin = io.stdin
  local result = table.pack(pcall(stdin.readLine, stdin, false))
  tty.window.handler = nil
  return select(2, assert(table.unpack(result)))
end

-- PLEASE do not use this method directly, use io.read or term.read
function tty.stream:read()
  local handler = tty.window.handler or {}
  local cursor = handler.cursor or tty.build_vertical_reader()

  tty.window.handler = nil
  handler.index = 0

  while true do
    local name, address, char, code = self:pull()
    -- we may have lost tty during the pull
    if not tty.isAvailable() then
      return
    end

    -- we have to keep checking what kb is active in case it is switching during use
    -- we could have multiple screens, each with keyboards active
    local main_kb = tty.keyboard()
    local main_sc = tty.screen()
    if name == "interrupted" then
      self:write("^C\n")
      return false, name
    elseif address == main_kb or address == main_sc then
      local handler_method = handler[name] or
      -- this handler listing hack is to delay load tty
        ({key_down=1, touch=1, drag=1, clipboard=1})[name] and tty[name .. "_handler"]
      if handler_method then
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

function tty.getCursor()
  local window = tty.window
  return window.x, window.y
end

function tty.setCursor(x, y)
  local window = tty.window
  window.x, window.y = x, y
end

-- PLEASE do not use this method directly, use io.write or term.write
function tty.stream:write(value)
  local gpu = tty.gpu()
  if not gpu then
    return
  end
  local window = tty.window
  local sy = 0
  local beeped
  local uptime = computer.uptime
  local last_sleep = uptime()
  while true do
    if uptime() - last_sleep > 1 then
      os.sleep(0)
      last_sleep = uptime()
    end

    local ansi_print = ""
    if window.ansi_escape then
      -- parse the instruction in segment
      -- [ (%d+;)+ %d+m
      window.ansi_escape = window.ansi_escape .. value
      local color_attributes = {tonumber(window.ansi_escape:match("^%[(%d%d)m"))}
      if not color_attributes[1] then
        color_attributes, ansi_print, value = require("vt100").parse(window)
      else
        value = window.ansi_escape:sub(5)
      end
      for _,catt in ipairs(color_attributes) do
        -- B6 is closer to cyan in 4 bit color
        local colors = {0x0,0xff0000,0x00ff00,0xffff00,0x0000ff,0xff00ff,0x00B6ff,0xffffff}
        catt = catt - 29
        local method = "setForeground"
        if catt > 10 then
          method = "setBackground"
          catt = catt - 10
        end
        local c = colors[catt]
        if c then
          gpu[method](c)
        end
        window.ansi_escape = nil -- might happen multiple times, that's fine
      end
    end

    -- scroll before parsing next line
    -- the value may only have been a newline
    sy = sy + self.scroll()
    -- we may have needed to scroll one last time [nowrap adjustments]
    if #value == 0 then
      break
    end

    local x, y = tty.getCursor()

    local _, ei, delim = unicode.sub(value, 1, window.width):find("([\27\t\r\n\a])", #ansi_print + 1)
    local segment = ansi_print .. (ei and value:sub(1, ei - 1) or value)

    if segment ~= "" then
      local gpu_x, gpu_y = x + window.dx, y + window.dy
      local tail = ""
      local wlen_needed = unicode.wlen(segment)
      local wlen_remaining = window.width - x + 1
      if wlen_remaining < wlen_needed then
        segment = unicode.wtrunc(segment, wlen_remaining + 1)
        local wlen_used = unicode.wlen(segment)
        -- we can clear the line because we already know remaining < needed
        tail = (" "):rep(wlen_remaining - wlen_used)
        if not window.nowrap then
          -- we have to reparse the delimeter
          ei = #segment
          -- fake a newline
          delim = "\n"
          wlen_needed = wlen_used
        end
      end
      gpu.set(gpu_x, gpu_y, segment..tail)
      x = x + wlen_needed
    end

    value = ei and value:sub(ei + 1) or ""

    if delim == "\t" then
      x = ((x-1) - ((x-1) % 8)) + 9
    elseif delim == "\r" or (delim == "\n" and not window.cr_last) then
      x = 1
      y = y + 1
    elseif delim == "\a" and not beeped then
      computer.beep()
      beeped = true
    elseif delim == "\27" then -- ansi escape
      window.ansi_escape = ""
    end

    tty.setCursor(x, y)
    window.cr_last = delim == "\r"
  end
  return sy
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
  if not window.gpu or window.gpu == gpu then
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

require("package").delay(tty, "/lib/core/full_tty.lua")

return tty
