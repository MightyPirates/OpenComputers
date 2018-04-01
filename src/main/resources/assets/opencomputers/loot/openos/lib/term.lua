local tty = require("tty")
local computer = require("computer")
local process = require("process")
local event = require("event")
local core_cursor = require("core/cursor")
local unicode = require("unicode")

local kb = require("keyboard")
local keys = kb.keys

local term = setmetatable({internal={}}, {__index=tty})

function term.internal.window()
  return process.info().data.window
end

local function as_window(window, func, ...)
  local data = process.info().data
  if not data.window or not window then
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
  local window = {fullscreen=select("#",...) == 0, blink = true, output_buffer = ""}

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

local function horizontal_echo(self, arg)
  if arg == "" then -- special scroll request
    local w = self.window
    local width = w.width
    if w.x >= width then
      -- render all text from 1 to width from end
      -- the width that matters depends on the following char width
      local next_overlap = math.max(unicode.wlen(unicode.sub(self.data, self.index + 1, self.index + 1)) - 1, 0)
      width = width - next_overlap
      if w.x > width then
        local s1, s2 =
          unicode.sub(self.data, self.vindex + 1, self.index),
          unicode.sub(self.data, self.index + 1)
        -- vindex is really the only way we know where input started
        local s1wlen = unicode.wlen(s1)
        -- adjust is how far to push the string back
        local adjust = w.x - width
        local wlen_available = s1wlen - adjust + 1 -- +1 because wtrunc removes the final position
        -- now we have to resize s2 to fit in wlen_available, from the end, not the front
        local trunc = unicode.wtrunc(unicode.reverse(s1), wlen_available)
        -- is it faster to reverse again, or take wlen and sub, probably just reverse
        trunc = unicode.reverse(trunc)
        -- a double wide may have just been cut
        local cutwlen = s1wlen - unicode.wlen(trunc)
        -- we have to move to position 1, which should be at vindex, or s2wlen back from x
        w.x = w.x - s1wlen
        self.output:write(trunc .. s2 .. (" "):rep(cutwlen))
        self.vindex = self.index - unicode.len(trunc)
        w.x = width - cutwlen + 1
      end
    elseif w.x < 1 then
      -- render all text from 1 to width from end
      local s2 = unicode.sub(self.data, self.index + 1)
      w.x = 1
      self.output:write(s2)
      w.x = 1
      self.vindex = self.index
    end
    -- scroll is safe now, return as normal below
  elseif arg == keys.left then
    if self.index < self.vindex then
      self.window.x = 0
      return self:echo("")
    end
  elseif arg == keys.right then
    self.window.x = self.window.x + unicode.wlen(unicode.sub(self.data, self.index, self.index))
    return self:echo("")
  end
  return self.super.echo(self, arg)
end

local function horizontal_update(self, arg, back)
  if back then
    -- if we're just going to render arg and move back, and we're not wrapping, just render arg
    -- back may be more or less from current x
    local arg_len = unicode.len(arg)
    local x = self.window.x
    self.output:write(arg)
    self.window.x = x
    self.data = self.data .. arg
    self.len = self.len + arg_len -- recompute len
    self:move(self.len - self.index + back) -- back is negative from end
    return true
  end
  return self.super.update(self, arg, back)
end

local function add_cursor_handlers(cursor, ops)
  -- cursor inheritance gives super: to access base methods
  -- function cursor:method(...)
  --   return self.super.method(self, ...)
  -- end
  local filter = ops.filter or cursor.filter
  if filter then
    if type(filter) == "string" then
      local filter_text = filter
      filter = function(text)
        return text:match(filter_text)
      end
    end

    function cursor:handle(name, char, code)
      if name == "key_down" and (code == keys.enter or code == keys.numpadenter) then
        if not filter(self.data) then
          computer.beep(2000, 0.1)
          return true -- handled
        end
      end
      return self.super.handle(self, name, char, code)
    end
  end

  local pwchar = ops.pwchar or cursor.pwchar
  local nobreak = ops.dobreak == false or cursor.dobreak == false
  if pwchar or nobreak or cursor.nowrap then
    if type(pwchar) == "string" then
      local pwchar_text = pwchar
      pwchar = function(text)
        return text:gsub(".", pwchar_text)
      end
    end  
    function cursor:echo(arg)
      if pwchar and type(arg) == "string" and #arg > 0 then -- "" is used for scrolling
        arg = pwchar(arg)
      elseif nobreak and arg == keys.enter then
        arg = ""
      end
      local echo = cursor.nowrap and horizontal_echo or self.super.echo
      return echo(self, arg)
    end
  end
  if cursor.nowrap then
    cursor.update = horizontal_update
    tty.window.nowrap = true
    cursor.vindex = 0 -- visual/virtual index
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
  local cursor = history or {}
  cursor.hint = cursor.hint or hint

  add_cursor_handlers(cursor, {
    dobreak = dobreak,
    pwchar = pwchar,
    filter = filter,
  })

  return tty.read(cursor)
end

function term.getGlobalArea()
  local w,h,dx,dy = tty.getViewport()
  return dx+1,dy+1,w,h
end

function term.clearLine()
  term.write("\27[2K\27[999D")
end

function term.setCursorBlink(enabled)
  tty.window.blink = enabled
end

function term.getCursorBlink()
  return tty.window.blink
end

function term.pull(...)
  local args = table.pack(...)
  local timeout = math.huge
  if type(args[1]) == "number" then
    timeout = computer.uptime() + table.remove(args, 1)
    args.n = args.n - 1
  end
  local cursor = core_cursor.new(nil, tty.window, tty.stream) -- cursors can blink (base arg is optional)
  while timeout >= computer.uptime() and cursor:echo() do
    local s = table.pack(event.pull(.5, table.unpack(args, 1, args.n)))
    cursor:echo(not s[1])
    if s.n > 1 then return table.unpack(s, 1, s.n) end
  end
end

function term.bind(gpu, window)
  return as_window(window, tty.bind, gpu)
end

term.scroll = tty.stream.scroll
term.internal.run_in_window = as_window

return term
