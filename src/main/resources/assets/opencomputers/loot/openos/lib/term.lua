local tty = require("tty")
local unicode = require("unicode")
local computer = require("computer")
local process = require("process")

local kb = require("keyboard")
local keys = kb.keys

-- tty is bisected into a delay loaded library
-- term indexing will fail to use full_tty unless tty is fully loaded
-- accessing tty.full_tty [a nonexistent field] will cause that full load
local term = setmetatable({internal={},tty.full_tty}, {__index=tty})

function term.internal.window()
  return process.info().data.window
end

local function as_window(window, func, ...)
  local data = process.info().data
  if not data.window then
    return func(...)
  end
  local prev = rawget(data, "window")
  data.window = window
  local ret = table.pack(func(...))
  data.window = prev
  return table.unpack(ret, 1, ret.n)
end

function term.internal.open(...)
  local dx, dy, w, h = ...
  local window = {fullscreen=select("#",...) == 0, blink = true}

  -- support legacy code using direct manipulation of w and h
  -- (e.g. wocchat) instead of using setViewport
  setmetatable(window,
  {
    __index = function(tbl, key)
      key = key == "w" and "width" or key == "h" and "height" or key
      return rawget(tbl, key)
    end,
    __newindex = function(tbl, key, value)
      key = key == "w" and "width" or key == "h" and "height" or key
      return rawset(tbl, key, value)
    end
  })

  -- first time we open a pty the current tty.window must become the process window
  if not term.internal.window() then
    local init_index = 2
    while process.info(init_index) do
      init_index = init_index + 1
    end
    process.info(init_index - 1).data.window = tty.window
    tty.window = nil
    setmetatable(tty,
    {
      __index = function(_, key)
        if key == "window" then
          return term.internal.window()
        end
      end
    })
  end

  as_window(window, tty.setViewport, w, h, dx, dy, 1, 1)
  return window
end

local function build_horizontal_reader(cursor)
  cursor.clear_tail = function(self)
    local w,_,dx,dy,x,y = tty.getViewport()
    local _,s2=tty.split(self)
    local wlen = math.min(unicode.wlen(s2),w-x+1)
    tty.gpu().fill(x+dx,y+dy,wlen,1," ")
  end
  cursor.move = function(self, n)
    local win = tty.window
    local a = self.index
    local b = math.max(0,math.min(unicode.len(self.data), self.index+n))
    self.index = b
    a, b = a < b and a or b, a < b and b or a
    local wlen_moved = unicode.wlen(unicode.sub(self.data, a + 1, b))
    win.x = win.x + wlen_moved * (n<0 and -1 or 1)
    self:scroll()
  end
  cursor.draw = function(_, text)
    local nowrap = tty.window.nowrap
    tty.window.nowrap = true
    tty.stream:write(text)
    tty.window.nowrap = nowrap
  end
  cursor.scroll = function(self, goback, prev_x)
    local win = tty.window
    win.x = goback and prev_x or win.x
    local x = win.x
    local w = win.width
    local data,px,i = self.data, self.promptx, self.index
    local available = w-px+1
    if x > w then
      local blank
      if i == unicode.len(data) then
        available,blank=available-1," "
      else
        i,blank=i+1,""
      end
      data = unicode.sub(data,1,i)
      local rev = unicode.reverse(data)
      local ending = unicode.wtrunc(rev, available+1)
      data = unicode.reverse(ending)
      win.x = self.promptx
      self:draw(data..blank)
      -- wide chars may place the cursor not exactly at the end
      win.x = math.min(w, self.promptx + unicode.wlen(data))
    -- x could be negative, we scroll it back into view
    elseif x < self.promptx then
      data = unicode.sub(data, self.index+1)
      if unicode.wlen(data) > available then
        data = unicode.wtrunc(data,available+1)
      end
      win.x = self.promptx
      self:draw(data)
      win.x = math.max(px, math.min(w, x))
    end
  end
  cursor.clear = function(self)
    local win = tty.window
    local gpu, px = win.gpu, self.promptx
    local w,_,dx,dy,_,y = tty.getViewport()
    self.index, self.data, win.x = 0, "", px
    gpu.fill(px+dx,y+dy,w-px+1-dx,1," ")
  end
end

local function inject_filter(handler, filter)
  if filter then
    if type(filter) == "string" then
      local filter_text = filter
      filter = function(text)
        return text:match(filter_text)
      end
    end

    handler.key_down = function(self, cursor, char, code)
      if code == keys.enter or code == keys.numpadenter then
        if not filter(cursor.data) then
          computer.beep(2000, 0.1)
          return false -- ignore
        end
      end
      return tty.key_down_handler(self, cursor, char, code)
    end
  end
end

local function inject_mask(cursor, dobreak, pwchar)
  if not pwchar and dobreak ~= false then
    return
  end

  if pwchar then
    if type(pwchar) == "string" then
      local pwchar_text = pwchar
      pwchar = function(text)
        return text:gsub(".", pwchar_text)
      end
    end
  end

  local cursor_draw = cursor.draw
  cursor.draw = function(self, text)
    local pre, newline = text:match("(.-)(\n?)$")
    if dobreak == false then
      newline = ""
    end
    if pwchar then
      pre = pwchar(pre)
    end
    return cursor_draw(self, pre .. newline)
  end
end

-- cannot use term.write = io.write because io.write invokes metatable
function term.write(value, wrap)
  local previous_nowrap = tty.window.nowrap
  tty.window.nowrap = wrap == false
  io.write(value)
  io.stdout:flush()
  tty.window.nowrap = previous_nowrap
end

function term.read(history, dobreak, hint, pwchar, filter)
  history = history or {}
  local handler = history
  handler.hint = handler.hint or hint

  local cursor = tty.build_vertical_reader()
  if handler.nowrap then
    build_horizontal_reader(cursor)
  end

  inject_filter(handler, filter)
  inject_mask(cursor, dobreak, pwchar or history.pwchar)
  handler.cursor = cursor

  return tty.read(handler)
end

function term.getGlobalArea(window)
  local w,h,dx,dy = as_window(window, tty.getViewport)
  return dx+1,dy+1,w,h
end

function term.clearLine(window)
  window = window or tty.window
  local w, h, dx, dy, _, y = as_window(window, tty.getViewport)
  window.gpu.fill(dx + 1, dy + math.max(1, math.min(y, h)), w, 1, " ")
  window.x = 1
end

function term.setCursorBlink(enabled)
  tty.window.blink = enabled
end

function term.getCursorBlink()
  return tty.window.blink
end

function term.pull(...)
  local args = table.pack(...)
  local timeout = nil
  if type(args[1]) == "number" then
    timeout = table.remove(args, 1)
    args.n = args.n - 1
  end
  local stdin_stream = io.stdin.stream
  if stdin_stream.pull then
    return stdin_stream:pull(timeout, table.unpack(args, 1, args.n))
  end
  -- if stdin does not have pull() we can build the result
  local result = io.read(1)
  if result then
    return "clipboard", nil, result
  end
end

function term.bind(gpu, window)
  return as_window(window, tty.bind, gpu)
end

function term.scroll(...)
  if io.stdout.tty then
    return io.stdout.stream.scroll(...)
  end
end

term.internal.run_in_window = as_window

return term
