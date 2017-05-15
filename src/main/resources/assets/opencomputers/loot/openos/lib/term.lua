local tty = require("tty")
local unicode = require("unicode")
local computer = require("computer")
local process = require("process")

local kb = require("keyboard")
local keys = kb.keys

local term = setmetatable({internal={}}, {__index=tty})

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
  return table.unpack(ret, ret.n)
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
      __index = function(tbl, key)
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
  cursor.clear_tail = function(_)
    local w,h,dx,dy,x,y = tty.getViewport()
    local s1,s2=tty.internal.split(_)
    local wlen = math.min(unicode.wlen(s2),w-x+1)
    tty.gpu().fill(x+dx,y+dy,wlen,1," ")
  end
  cursor.move = function(_,n)
    local win = tty.window
    local a = _.index
    local b = math.max(0,math.min(unicode.len(_.data),_.index+n))
    _.index = b
    a,b = a<b and a or b,a<b and b or a
    local wlen_moved = unicode.wlen(unicode.sub(_.data,a+1,b))
    win.x = win.x + wlen_moved * (n<0 and -1 or 1)
    _:scroll()
  end
  cursor.draw = function(_, text)
    tty.drawText(text, true)
  end
  cursor.scroll = function(_)
    local win = tty.window
    local gpu,data,px,i = win.gpu,_.data,_.promptx,_.index
    local w,h,dx,dy,x,y = tty.getViewport()
    win.x = math.max(_.promptx, math.min(w, x))
    local len = unicode.len(data)
    local available,sx,sy,last = w-px+1,px+dx,y+dy,i==len
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
      gpu.set(sx,sy,data..blank)
      win.x=math.min(w,_.promptx+unicode.wlen(data))
    elseif x < _.promptx then
      data = unicode.sub(data,_.index+1)
      if unicode.wlen(data) > available then
        data = unicode.wtrunc(data,available+1)
      end
      gpu.set(sx,sy,data)
    end
  end
  cursor.clear = function(_)
    local win = tty.window
    local gpu,data,px=win.gpu,_.data,_.promptx
    local w,h,dx,dy,x,y = tty.getViewport()
    _.index,_.data,win.x=0,"",px
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

    local mt =
    {
      __newindex = function(tbl, key, value)
        if key == "key_down" then
          local tty_key_down = value
          value = function(handler, cursor, char, code)
            if code == keys.enter or code == keys.numpadenter then
              if not filter(cursor.data) then
                computer.beep(2000, 0.1)
                return false -- ignore
              end
            end
            return tty_key_down(handler, cursor, char, code)
          end
        end
        rawset(tbl, key, value)
      end
    }
    setmetatable(handler, mt)
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
  cursor.draw = function(cursor, text)
    local pre, newline = text:match("(.-)(\n?)$")
    if dobreak == false then
      newline = ""
    end
    if pwchar then
      pre = pwchar(pre)
    end
    return cursor_draw(cursor, pre .. newline)
  end
end

function term.read(history, dobreak, hint, pwchar, filter)
  if not io.stdin.tty then
    return io.read()
  end
  local handler = history or {}
  handler.hint = handler.hint or hint

  local cursor = tty.internal.build_vertical_reader()
  if handler.nowrap then
    build_horizontal_reader(cursor)
  end

  inject_filter(handler, filter)
  inject_mask(cursor, dobreak, pwchar)

  return tty.read(handler, cursor)
end

function term.getGlobalArea(window)
  local w,h,dx,dy = as_window(window, tty.getViewport)
  return dx+1,dy+1,w,h
end

function term.clearLine(window)
  window = window or tty.window
  local w,h,dx,dy,x,y = as_window(window, tty.getViewport)
  window.gpu.fill(dx+1,dy+math.max(1,math.min(y,h)),w,1," ")
  window.x=1
end

function term.pull(...)
  local args = table.pack(...)
  local timeout = nil
  if type(args[1]) == "number" then
    timeout = table.remove(args, 1)
    args.n = args.n - 1
  end
  return tty.pull(nil, timeout, table.unpack(args, 1, args.n))
end

function term.bind(gpu, window)
  return as_window(window, tty.bind, gpu)
end

return term

