local tty = require("tty")
local computer = require("computer")
local process = require("process")
local event = require("event")
local core_cursor = require("core/cursor")

local kb = require("keyboard")
local keys = kb.keys

local term = setmetatable({internal={}}, {__index=tty})

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
  if rawget(tty, "window") then
    for _,p in pairs(process.list) do
      if not p.parent then
        p.data.window = tty.window
        break
      end
    end
    tty.window = nil
    setmetatable(tty,
    {
      __index = function(_, key)
        if key == "window" then
          return process.info().data.window
        end
      end
    })
  end

  as_window(window, tty.setViewport, w, h, dx, dy, 1, 1)
  as_window(window, tty.bind, tty.gpu())
  return window
end

local function create_cursor(history, ops)
  local cursor = history or {}
  cursor.hint = ops.hint or cursor.hint

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
  if pwchar or nobreak then
    if type(pwchar) == "string" then
      local pwchar_text = pwchar
      pwchar = function(text)
        return text:gsub(".", pwchar_text)
      end
    end  
    function cursor:echo(arg, ...)
      if pwchar and type(arg) == "string" and #arg > 0 and not arg:match("^\27") then -- "" is used for scrolling
        arg = pwchar(arg)
      elseif nobreak and arg == "\n" then
        arg = ""
      end
      return self.super.echo(self, arg, ...)
    end
  end

  return core_cursor.new(cursor, cursor.nowrap and core_cursor.horizontal)
end

-- cannot use term.write = io.write because io.write invokes metatable
function term.write(value, wrap)
  io.stdout:flush()
  local previous_nowrap = tty.window.nowrap
  tty.window.nowrap = wrap == false
  io.write(value)
  io.stdout:flush()
  tty.window.nowrap = previous_nowrap
end

function term.read(history, dobreak, hint, pwchar, filter)
  tty.window.cursor = create_cursor(history, {
    dobreak = dobreak,
    pwchar = pwchar,
    filter = filter,
    hint = hint
  })
  return io.stdin:readLine(false)
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
  local blink = tty.window.blink
  -- If tty.window.blink is internal, move this to term.setCursorBlink()
  if not blink or blink == 0 then blink = math.huge
  elseif type(blink) ~= 'number' then blink = .5
  elseif blink < 0 then return event.pull(...) end

  local args = table.pack(...)
  local timeout = math.huge
  if type(args[1]) == "number" then
    timeout = computer.uptime() + table.remove(args, 1)
    args.n = args.n - 1
  end
  local cursor = core_cursor.new()
  local next_blink = math.min(timeout, computer.uptime() + blink)
  cursor:echo()
  while timeout >= computer.uptime() do
    local s = table.pack(event.pull(next_blink - computer.uptime(), table.unpack(args, 1, args.n)))
    if s.n > 0 then
      cursor:echo(false)
      return table.unpack(s, 1, s.n)
    end
    if computer.uptime() >= next_blink then
      cursor:echo(true)
      next_blink = math.min(timeout, computer.uptime() + blink)
    end
  end
end

function term.bind(gpu, window)
  return as_window(window, tty.bind, gpu)
end

term.scroll = tty.stream.scroll
term.internal.run_in_window = as_window

return term
