local unicode = require("unicode")
local event = require("event")
local process = require("process")
local kb = require("keyboard")
local component = require("component")
local keys = kb.keys

local term = {}
term.internal = {}

function term.internal.window()
  return process.info().data.window
end

local W = term.internal.window

local local_env = {unicode=unicode,event=event,process=process,W=W,kb=kb}

function term.internal.open(...)
  local dx, dy, w, h = ...
  local window = {x=1,y=1,fullscreen=select("#",...)==0,dx=dx or 0,dy=dy or 0,w=w,h=h,blink=true}
  event.listen("screen_resized", function(_,addr,w,h)
    if term.isAvailable(window) and term.screen(window) == addr and window.fullscreen then
      window.w,window.h = w,h
    end
  end)
  return window
end

function term.getViewport(window)
  window = window or W()
  return window.w, window.h, window.dx, window.dy, window.x, window.y
end

function term.setViewport(w,h,dx,dy,x,y,window)
  window = window or W()

  local gw,gh = window.gpu.getViewport()
  w,h,dx,dy,x,y = w or gw,h or gh,dx or 0,dy or 0,x or 1,y or 1

  window.w,window.h,window.dx,window.dy,window.x,window.y,window.gw,window.gh=
    w,h,dx,dy,x,y, gw, gh
end

function term.gpu(window)
  window = window or W()
  return window.gpu
end

function term.clear()
  local w = W()
  local gpu = w.gpu
  if not gpu then return end
  gpu.fill(1+w.dx,1+w.dy,w.w,w.h," ")
  w.x,w.y=1,1
end

function term.isAvailable(w)
  w = w or W()
  return w and not not (w.gpu and w.gpu.getScreen())
end

function term.internal.pull(input, c, off, t, ...)
  t=t or math.huge
  if t < 0 then return end
  local w,unpack=W(),table.unpack
  local d,h,dx,dy,x,y=term.getViewport(w)
  local out = (x<1 or x>d or y<1 or y>h)
  if input and out then
    input:move(0)
    y=w.y
    input:scroll()
  end
  x,y=w.x+dx,w.y+dy
  local gpu

  if input or not out then
    gpu=w.gpu
    local sf,sb=gpu.setForeground,gpu.setBackground
    c=c or {{gpu.getBackground()},{gpu.getForeground()},gpu.get(x,y)}
    local c11,c12 = unpack(c[1])
    local c21,c22 = unpack(c[2])
    -- c can fail if gpu does not have a screen
    -- if can happen during a type of race condition when a screen is removed
    if not c11 then
      return nil, "interrupted"
    end
    if not off then
      sf(c11,c12)
      sb(c21,c22)
    end
    gpu.set(x,y,c[3])
    sb(c11,c12)
    sf(c21,c22)
  end

  local a={event.pull(math.min(t,0.5),...)}

  if #a>1 or t<.5 then
    if gpu then
      gpu.set(x,y,c[3])
    end
    return unpack(a)
  end
  local blinking = w.blink
  if input then blinking = input.blink end
  return term.internal.pull(input,c,blinking and not off,t-0.5,...)
end

function term.pull(p,...)
  local a,t = {p,...}
  if type(p) == "number" then t = table.remove(a,1) end
  return term.internal.pull(nil,nil,nil,t,table.unpack(a))
end

function term.read(history,dobreak,hintHandler,pwchar,filter)
  if not io.stdin.tty then return io.read() end
  local ops = history or {}
  ops.dobreak = ops.dobreak
  if ops.dobreak==nil then ops.dobreak = dobreak end
  ops.hintHandler = ops.hintHandler or hintHandler
  ops.pwchar = ops.pwchar or pwchar
  ops.filter = ops.filter or filter
  return term.readKeyboard(ops)
end

function term.internal.split(input)
  local data,index=input.data,input.index
  local dlen = unicode.len(data)
  index=math.max(0,math.min(index,dlen))
  local tail=dlen-index
  return unicode.sub(data,1,index),tail==0 and""or unicode.sub(data,-tail)
end

function term.internal.build_vertical_reader(input)
  input.sy = 0
  input.scroll = function(_)
    _.sy = _.sy + term.internal.scroll(_.w)
    _.w.y = math.min(_.w.y,_.w.h)
  end
  input.move = function(_,n)
    local w=_.w
    _.index = math.min(math.max(0,_.index+n),unicode.len(_.data))
    local s1,s2 = term.internal.split(_)
    s2 = unicode.sub(s2.." ",1,1)
    local data_remaining = ("_"):rep(_.promptx-1)..s1..s2
    w.y = _.prompty - _.sy
    while true do
      local wlen_remaining = unicode.wlen(data_remaining)
      if wlen_remaining > w.w then
        local line_cut = unicode.wtrunc(data_remaining, w.w+1)
        data_remaining = unicode.sub(data_remaining,unicode.len(line_cut)+1)
        w.y=w.y+1
      else
        w.x = wlen_remaining-unicode.wlen(s2)+1
        break
      end
    end
  end
  input.clear_tail = function(_)
    local win=_.w
    local oi,w,h,dx,dy,ox,oy = _.index,term.getViewport(win)
    _:move(math.huge)
    _:move(-1)
    local ex,ey=win.x,win.y
    win.x,win.y,_.index=ox,oy,oi
    x=oy==ey and ox or 1
    win.gpu.fill(x+dx,ey+dy,w-x+1,1," ")
  end
  input.update = function(_,arg)
    local w,cursor,suffix=_.w
    local s1,s2=term.internal.split(_)
    if type(arg) == "number" then
      local ndata
      if arg < 0 then if _.index<=0 then return end
        _:move(-1)
        ndata=unicode.sub(s1,1,-2)..s2
      else if _.index>=unicode.len(_.data) then return end
        s2=unicode.sub(s2,2)
        ndata=s1..s2
      end
      suffix=s2
      input:clear_tail()
      _.data = ndata
    else
      _.data=s1..arg..s2
      _.index=_.index+unicode.len(arg)
      cursor,suffix=arg,s2
    end
    if cursor then _:draw(_.mask(cursor)) end
    if suffix and suffix~="" then
      local px,py,ps=w.x,w.y,_.sy
      _:draw(_.mask(suffix))
      w.x,w.y=px,py-(_.sy-ps)
    end
  end
  input.clear = function(_)
    _:move(-math.huge)
    _:draw((" "):rep(unicode.wlen(_.data)))
    _:move(-math.huge)
    _.index=0
    _.data=""
  end
  input.draw = function(_,text)
    _.sy = _.sy + term.drawText(text,true)
  end
end

function term.internal.read_history(history,input,change)
  if not change then
    if unicode.wlen(input.data) > 0 then
      table.insert(history.list,1,input.data)
      history.list[(tonumber(os.getenv("HISTSIZE")) or 10)+1]=nil
      history.list[0]=nil
    end
  else
    local ni = history.index + change
    if ni >= 0 and ni <= #history.list then
      history.list[history.index]=input.data
      history.index = ni
      input:clear()
      input:update(history.list[ni])
    end
  end
end

function term.readKeyboard(ops)
  checkArg(1,ops,"table")
  local filter = ops.filter and function(i) return term.internal.filter(ops.filter,i) end or term.internal.nop
  local pwchar = ops.pwchar and function(i) return term.internal.mask(ops.pwchar,i) end or term.internal.nop
  local history,db,hints={list=ops,index=0},ops.dobreak,{handler=ops.hintHandler}
  local w=W()
  local draw=io.stdin.tty and term.drawText or term.internal.nop
  local input={w=w,promptx=w.x,prompty=w.y,index=0,data="",mask=pwchar}
  input.blink = ops.blink
  if input.blink == nil then
    input.blink = w.blink
  end

  -- two wrap types currently supported, vertical and hortizontal
  if ops.nowrap then term.internal.build_horizontal_reader(input)
  else               term.internal.build_vertical_reader(input)
  end

  while true do
    local name, address, char, code = term.internal.pull(input)
    if not term.isAvailable() then
      return
    end

    -- we have to keep checking what kb is active in case it is switching during use
    -- we could have multiple screens, each with keyboards active
    local main_kb = term.keyboard(w)
    local main_sc = term.screen(w)
    local c
    local backup_cache = hints.cache
    if name == "interrupted" then
      draw("^C\n",true)
      return false
    elseif address == main_kb or address == main_sc then
      if name == "touch" or name == "drag" then
        term.internal.onTouch(input,char,code)
      elseif name == "clipboard" then
        c = term.internal.clipboard(char)
        hints.cache = nil
      elseif name == "key_down" then
        hints.cache = nil
        local ctrl = kb.isControlDown(address)
        if ctrl and code == keys.d then return
        elseif char == 9 then
          hints.cache = backup_cache
          term.internal.tab(input,hints)
        elseif char == 13 and filter(input) then
          input:move(math.huge)
          if db ~= false then
            draw("\n")
          end
          term.internal.read_history(history,input)
          return input.data .. "\n"
        elseif code == keys.up     then term.internal.read_history(history, input,  1)
        elseif code == keys.down   then term.internal.read_history(history, input, -1)
        elseif code == keys.left   then input:move(ctrl and term.internal.ctrl_movement(input, -1) or -1)
        elseif code == keys.right  then input:move(ctrl and term.internal.ctrl_movement(input,  1) or  1)
        elseif code == keys.home   then input:move(-math.huge)
        elseif code == keys["end"] then input:move( math.huge)
        elseif code == keys.back   then c = -1
        elseif code == keys.delete then c =  0
        elseif char >= 32          then c = unicode.char(char)
        elseif ctrl and char == "w"then -- TODO: cut word
        else                            hints.cache = backup_cache -- ignored chars shouldn't clear hint cache
        end
      end
      -- if we obtained something (c) to handle
      if c then
        input:update(c)
      end
    end
  end
end

-- cannot use term.write = io.write because io.write invokes metatable
function term.write(value,wrap)
  local stdout = io.output()
  local stream = stdout and stdout.stream
  local previous_wrap = stream.wrap
  stream.wrap = wrap == nil and true or wrap
  stdout:write(value)
  stdout:flush()
  stream.wrap = previous_wrap
end

function term.getCursor()
  local w = W()
  return w.x,w.y
end

function term.setCursor(x,y)
  local w = W()
  w.x,w.y=x,y
end

function term.drawText(value, wrap, window)
  window = window or W()
  if not window then return end
  local gpu = window.gpu
  if not gpu then return end
  local w,h,dx,dy,x,y = term.getViewport(window)
  local sy = 0
  local vlen = #value
  local index = 1
  local cr_last,beeped = false,false
  local function scroll(_sy,_y)
    return _sy + term.internal.scroll(window,_y-h), math.min(_y,h)
  end
  while index <= vlen do
    local si,ei = value:find("[\t\r\n\a]", index)
    si = si or vlen+1
    if index==si then
      local delim = value:sub(index, index)
      if delim=="\t" then
        x=((x-1)-((x-1)%8))+9
      elseif delim=="\r" or (delim=="\n" and not cr_last) then
        x,y=1,y+1
        sy,y = scroll(sy,y)
      elseif delim=="\a" and not beeped then
        require("computer").beep()
        beeped = true
      end
      cr_last = delim == "\r"
    else
      sy,y = scroll(sy,y)
      si = si - 1
      local next = value:sub(index, si)
      local wlen_needed = unicode.wlen(next)
      local slen = #next
      local wlen_remaining = w - x + 1
      local clean_end = ""
      if wlen_remaining < wlen_needed then
        next = unicode.wtrunc(next, wlen_remaining + 1)
        wlen_needed = unicode.wlen(next)
        clean_end = (" "):rep(wlen_remaining-wlen_needed)
        if not wrap then
          si = math.huge
        end
      end
      gpu.set(x+dx,y+dy,next..clean_end)
      x = x + wlen_needed
      if wrap and slen ~= #next then
        si = si - (slen - #next)
        x = 1
        y = y + 1
      end
    end
    index = si + 1
  end

  window.x,window.y = x,y
  return sy
end

function term.internal.scroll(w,n)
  w = w or W()
  local gpu,d,h,dx,dy,x,y = w.gpu,term.getViewport(w)
  n = n or (y-h)
  if n <= 0 then return 0 end
  gpu.copy(dx+1,dy+n+1,d,h-n,0,-n)
  gpu.fill(dx+1,dy+h-n+1,d,n," ")
  return n
end

function term.internal.nop(...)
  return ...
end

function term.setCursorBlink(enabled)
  W().blink=enabled
end

function term.getCursorBlink()
  return W().blink
end

function term.bind(gpu, window)
  window = window or W()
  window.gpu = gpu or window.gpu
  window.keyboard = nil -- without a keyboard bound, always use the screen's main keyboard (1st)
  if window.fullscreen then
    term.setViewport(nil,nil,nil,nil,window.x,window.y,window)
  end
end

function term.keyboard(window)
  window = window or W() or {} -- this method needs to be safe even if there is no terminal window (e.g. no gpu)

  if window.keyboard then
    return window.keyboard
  end

  local system_keyboard = component.isAvailable("keyboard") and component.keyboard
  system_keyboard = system_keyboard and system_keyboard.address or "no_system_keyboard"

  local screen = term.screen(window)

  if not screen then
    -- no screen, no known keyboard, use system primary keyboard if any
    return system_keyboard
  end

  -- if we are using a gpu bound to the primary scren, then use the primary keyboard
  if component.isAvailable("screen") and component.screen.address == screen then
    window.keyboard = system_keyboard
  else
    -- calling getKeyboards() on the screen is costly (time)
    -- custom terminals should avoid designs that require
    -- this on every key hit

    -- this is expensive (slow!)
    window.keyboard = component.invoke(screen, "getKeyboards")[1] or system_keyboard
  end

  return window.keyboard
end

function term.screen(window)
  window = window or W()
  local gpu = window.gpu
  if not gpu then
    return nil
  end
  return gpu.getScreen()
end

function --[[@delayloaded-start@]] term.scroll(number, window)
  -- if zero scroll length is requested, do nothing
  if number == 0 then return end
  -- window is optional, default to current active terminal
  window = window or W()
  -- gpu works with global coordinates
  local gpu,width,height,dx,dy,x,y = window.gpu,term.getViewport(w)

  -- scroll request can be too large
  local abs_number = math.abs(number)
  if (abs_number >= height) then
    term.clear()
    return
  end

  -- box positions to shift
  local box_height = height - abs_number
  local top = 0
  if number > 0 then
    top = number -- (e.g. 1 scroll moves box at 2)
  end

  gpu.copy(dx + 1, dy + top + 1, width, box_height, 0, -number)

  local fill_top = 0
  if number > 0 then
    fill_top = box_height
  end

  gpu.fill(dx + 1, dy + fill_top + 1, width, abs_number, " ")
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.ctrl_movement(input, dir)
  local index, data = input.index, input.data

  local function isEdge(char)
    return char == "" or not not char:find("%s")
  end

  local last=dir<0 and 0 or unicode.len(data)
  local start=index+dir+1
  for i=start,last,dir do
    local a,b = unicode.sub(data, i-1, i-1), unicode.sub(data, i, i)
    if isEdge(a) and not isEdge(b) then return i-(index+1) end
  end
  return last - index
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.onTouch(input,gx,gy)
  if input.data == "" then return end
  input:move(-math.huge)
  local w = W()
  gx,gy=gx-w.dx,gy-w.dy
  local x2,y2,d = input.w.x,input.w.y,input.w.w
  local char_width_to_move = ((gy*d+gx)-(y2*d+x2))
  if char_width_to_move <= 0 then return end
  local total_wlen = unicode.wlen(input.data)
  if char_width_to_move >= total_wlen then
    input:move(math.huge)
  else
    local chars_to_move = unicode.wtrunc(input.data, char_width_to_move + 1)
    input:move(unicode.len(chars_to_move))
  end
  -- fake white space can make the index off, redo adjustment for alignment
  x2,y2,d = input.w.x,input.w.y,input.w.w
  char_width_to_move = ((gy*d+gx)-(y2*d+x2))
  if (char_width_to_move < 0) then
    -- using char_width_to_move as a type of index is wrong, but large enough and helps to speed this up
    local up_to_cursor = unicode.sub(input.data, input.index+char_width_to_move, input.index)
    local full_wlen = unicode.wlen(up_to_cursor)
    local without_tail = unicode.wtrunc(up_to_cursor, full_wlen + char_width_to_move + 1)
    local chars_cut = unicode.len(up_to_cursor) - unicode.len(without_tail)
    input:move(-chars_cut)
  end
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.build_horizontal_reader(input)
  term.internal.build_vertical_reader(input)
  input.clear_tail = function(_)
    local w,h,dx,dy,x,y = term.getViewport(_.w)
    local s1,s2=term.internal.split(_)
    local wlen = math.min(unicode.wlen(s2),w-x+1)
    _.w.gpu.fill(x+dx,y+dy,wlen,1," ")
  end
  input.move = function(_,n)
    local win = _.w
    local a = _.index
    local b = math.max(0,math.min(unicode.len(_.data),_.index+n))
    _.index = b
    a,b = a<b and a or b,a<b and b or a
    local wlen_moved = unicode.wlen(unicode.sub(_.data,a+1,b))
    win.x = win.x + wlen_moved * (n<0 and -1 or 1)
    _:scroll()
  end
  input.draw = function(_,text)
    term.drawText(text,false)
  end
  input.scroll = function(_)
    local win = _.w
    local gpu,data,px,i = win.gpu,_.data,_.promptx,_.index
    local w,h,dx,dy,x,y = term.getViewport(win)
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
  input.clear = function(_)
    local win = _.w
    local gpu,data,px=win.gpu,_.data,_.promptx
    local w,h,dx,dy,x,y = term.getViewport(win)
    _.index,_.data,win.x=0,"",px
    gpu.fill(px+dx,y+dy,w-px+1-dx,1," ")
  end
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] term.clearLine(window)
  window = window or W()
  local w,h,dx,dy,x,y = term.getViewport(window)
  window.gpu.fill(dx+1,dy+math.max(1,math.min(y,h)),w,1," ")
  window.x=1
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.mask(mask,input)
  if not mask then return input end
  if type(mask) == "function" then return mask(input) end
  return mask:rep(unicode.wlen(input))
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.filter(filter,input)
  if not filter then return true
  elseif type(filter) == "string" then return input.data:match(filter)
  elseif filter(input.data) then return true
  else require("computer").beep(2000, 0.1) end
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.tab(input,hints)
  if not hints.handler then return end
  if not hints.cache then
    hints.cache = type(hints.handler)=="table" and hints.handler
      or hints.handler(input.data,input.index + 1) or {}
    hints.cache.i=-1
  end
  local c=hints.cache
  local main_kb = term.keyboard()
  -- term may not have a keyboard
  -- in which case, we shouldn't be handling tab events
  if not main_kb then
    return
  end
  local change = kb.isShiftDown(main_kb) and -1 or 1
  c.i=(c.i+change)%math.max(#c,1)
  local next=c[c.i+1]
  if next then
    local tail = unicode.len(input.data) - input.index
    input:clear()
    input:update(next)
    input:move(-tail)
  end
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] term.getGlobalArea(window)
  local w,h,dx,dy = term.getViewport(window)
  return dx+1,dy+1,w,h
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] term.internal.clipboard(char)
  local first_line, end_index = char:find("\13?\10")
  if first_line then
    local after = char:sub(end_index + 1)
    if after ~= "" then
      require("computer").pushSignal("key_down", term.keyboard(), 13, 28)
      require("computer").pushSignal("clipboard", term.keyboard(), after)
    end
    char = char:sub(1, first_line - 1)
  end
  return char
end --[[@delayloaded-end@]] 

return term, local_env
