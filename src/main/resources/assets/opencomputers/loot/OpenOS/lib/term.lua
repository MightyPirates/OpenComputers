local component = require("component")
local computer = require("computer")
local event = require("event")
local keyboard = require("keyboard")
local text = require("text")
local unicode = require("unicode")

local term = {}
local methods = {}

local validateFocus
local metatable = {
  __index = methods,
  __pairs = function(self)
    return function(_, k)
      return next(methods, k)
    end, self, nil
  end
}

--allows collection of windows with enabled cursor blink
--timer id -> window
term.__blinking = setmetatable({}, {__mode = "v"})

--screen address -> window
term.__focus = setmetatable({}, {__mode = "v"})
term.__nextFocus = setmetatable({}, {__mode = "v"})
--window -> screen address
term.__usedScreen = setmetatable({}, {__mode = "k"})
--
term.__knownWindows = setmetatable({}, {
  __mode = "v",
  __index = function(t, k)
    local v = setmetatable({}, {__mode = "v"})
    t[k] = v
    return v
  end,
})

local function register(self, address)
  local state = self.__state
  if address then
    state.neighbors = term.__knownWindows[address]
    table.insert(state.neighbors, self)
  else
    state.neighbors = {self}
  end
end

local function unregister(self, address)
  local state = self.__state
  local i, n = 1, #state.neighbors
  for i = 1, n do
    if state.neighbors[i] == self then
      table.remove(state.neighbors, i)
      break
    end
  end
  state.neighbors = nil
end

local function setFocus(self, screenAddress)
  if screenAddress and term.__focus[screenAddress] ~= self and term.__nextFocus[screenAddress] ~= self then
    term.__focus[screenAddress] = nil
    term.__nextFocus[screenAddress] = self
    computer.pushSignal("term_focus", screenAddress)
  end
end

validateFocus = function(self, address)
  local oldAddress = term.__usedScreen[self]
  if oldAddress ~= address then
    self:unfocus()
    unregister(self, oldAddress)
    term.__usedScreen[self] = address
    register(self, address)
  end
  if address then
    if term.__focus[address] == nil and term.__nextFocus[address] == nil then
      setFocus(self, address)
    end
  end
end

local function gpu(self)
  local state = self.__state
  return state.gpu or (component.isAvailable("gpu") and component.gpu) or nil
end

local function getScreenAddress(self)
  if gpu(self) then
    local ok, address = pcall(gpu(self).getScreen)
    if ok then
      validateFocus(self, address)
      return address
    end
    validateFocus(self, nil)
  end
end

local function toggleBlink(self)
  local state = self.__state
  local cursorBlink = state.cursorBlink
  local screenAddress = getScreenAddress(self)
  if screenAddress then
    local currentFocus = term.__focus[screenAddress]
    local isInFocus = (currentFocus == self or currentFocus == nil)
    cursorBlink.state = not cursorBlink.state
    local x, y, w, h = self:getGlobalArea()
    local cursorX, cursorY = state.cursorX, state.cursorY
    if cursorX < 1 or cursorY < 1 or cursorX > w or cursorY > h then
      return
    end
    local xAbs, yAbs = x + cursorX - 1, y + cursorY - 1
    if cursorBlink.state then
      local char = isInFocus and unicode.char(0x2588) or unicode.char(0x2592) -- solid block or medium shade
      cursorBlink.alt = gpu(self).get(xAbs, yAbs) or cursorBlink.alt
      gpu(self).set(xAbs, yAbs, string.rep(char, unicode.charWidth(cursorBlink.alt)))
    else
      gpu(self).set(xAbs, yAbs, cursorBlink.alt)
    end
  end
end

term.__keyboardToScreen = {}
term.__screenToKeyboard = {}

-------------------------------------------------------------------------------

function term.newWindow(x, y, width, height, gpu)
  checkArg(1, x, "number", "nil")
  checkArg(2, y, "number", "nil")
  checkArg(3, width, "number", "nil")
  checkArg(4, height, "number", "nil")
  checkArg(5, gpu, "table", "nil")
  local window = setmetatable({}, metatable)
  window.__state = {
    x = x or 1,
    y = y or 1,
    width = width or -1,
    height = height or -1,
    gpu = gpu,
    cursorX = 1, cursorY = 1,
    cursorBlink = nil,
    neighbors = {window},
  }
  return window
end

local defaultWindow = term.newWindow()

function term.setWindow(newWindow)
  checkArg(1, newWindow, "table")
  local info = require("process").info()
  local oldWindow
  if info then
    local data = info.data
    oldWindow = data.term_window
    data.term_window = newWindow
  else
    oldWindow = defaultWindow
    defaultWindow = newWindow
  end
  return oldWindow
end

function term.getWindow()
  local info = require("process").info()
  if info then
    local data = info.data
    if data.term_window then
      return data.term_window
    end
  end
  return defaultWindow
end


function methods:setArea(x, y, width, height)
  checkArg(1, x, "number")
  checkArg(2, y, "number")
  checkArg(3, width, "number")
  checkArg(4, height, "number")
  assert(x ~= 0 and y ~= 0 and width ~= 0 and height ~= 0, "Nonzero arguments only!")
  
  local state = self.__state
  local oldX, oldY, oldWidth, oldHeight = state.x, state.y, state.width, state.height
  state.x, state.y, state.width, state.height = x, y, width, height
  return oldX, oldY, oldWidth, oldHeight
end

function methods:getArea()
  local state = self.__state
  return state.x, state.y, state.width, state.height
end

function methods:setGPU(newGPU)
  local state = self.__state
  local oldGPU = state.gpu
  state.gpu = newGPU
  return oldGPU
end

function methods:getGPU()
  local state = self.__state
  return state.gpu, gpu(self)
end

local function getAbsolute(pos, size)
  if pos < 0 then
    pos = size + pos + 1
  end
  return math.min(math.max(pos, 1), size)
end

function methods:getGlobalArea(w, h)
  checkArg(1, w, "number", "nil")
  checkArg(2, h, "number", "nil")
  local state = self.__state
  if not w or not h then
    if gpu(self) then
      w, h = gpu(self).getViewport()
      validateFocus(self, gpu(self).getScreen())
    end
  end
  if not w then
    return
  end
  local x, y = getAbsolute(state.x, w), getAbsolute(state.y, h)
  local wMax, hMax = w - x + 1, h - y + 1
  return x, y, getAbsolute(state.width, wMax), getAbsolute(state.height, hMax)
end


function methods:toGlobal(x, y)
  checkArg(1, x, "number")
  checkArg(2, y, "number")
  local dx, dy = self:getGlobalArea()
  if dx then
    return x + dx - 1, y + dy - 1
  end
end
function methods:toLocal(x, y)
  checkArg(1, x, "number")
  checkArg(2, y, "number")
  local dx, dy = self:getGlobalArea()
  if dx then
    return x - dx + 1, y - dy + 1
  end
end

function methods:clear()
  local state = self.__state
  local x, y, w, h = self:getGlobalArea()
  if x then
    gpu(self).fill(x, y, w, h, " ")
  end
  state.cursorX, state.cursorY = 1, 1
end

function methods:reset()
  if self:isAvailable() then
    local maxw, maxh = gpu(self).maxResolution()
    gpu(self).setResolution(maxw, maxh)
    gpu(self).setBackground(0x000000)
    gpu(self).setForeground(0xFFFFFF)
    self:clear()
  end
end

function methods:clearLine()
  local state = self.__state
  local x, y, w, h = self:getGlobalArea()
  if x then
    gpu(self).fill(x, state.cursorY + y - 1, w, 1, " ")
  end
  state.cursorX = 1
end

function methods:getCursor()
  local state = self.__state
  return state.cursorX, state.cursorY
end

function methods:setCursor(col, row)
  checkArg(1, col, "number")
  checkArg(2, row, "number")
  local state = self.__state
  local cursorBlink = state.cursorBlink
  if cursorBlink and cursorBlink.state then
    toggleBlink(self)
  end
  state.cursorX = math.floor(col)
  state.cursorY = math.floor(row)
  local wide, right = self:isWide(state.cursorX, state.cursorY)
  if wide and right then
    state.cursorX = state.cursorX - 1
  end
end

function methods:getCursorBlink()
  local state = self.__state
  return state.cursorBlink ~= nil
end

function methods:setCursorBlink(enabled)
  checkArg(1, enabled, "boolean")
  local state = self.__state
  local cursorBlink = state.cursorBlink
  if enabled then
    if not cursorBlink then
      cursorBlink = {}
      cursorBlink.id = event.timer(0.5, function() toggleBlink(term.__blinking[cursorBlink.id]) end, math.huge)
      term.__blinking[cursorBlink.id] = self
      cursorBlink.state = false
    elseif not cursorBlink.state then
      toggleBlink(self)
    end
  elseif cursorBlink then
    term.__blinking[cursorBlink.id] = nil
    event.cancel(cursorBlink.id)
    if cursorBlink.state then
      toggleBlink(self)
    end
    cursorBlink = nil
  end
  state.cursorBlink = cursorBlink
end

function methods:isWide(x, y)
  local xWin, yWin, wWin, hWin = self:getGlobalArea()
  if xWin == nil then
    return false
  end
  if x < 1 or x > wWin or y < 1 or y > hWin then
    return false
  end
  x = x + xWin - 1
  y = y + yWin - 1
  local char = gpu(self).get(x, y)
  if unicode.isWide(char) then
    -- The char at the specified position is a wide char.
    return true
  end
  if char == " " and x > xWin then
    local charLeft = gpu(self).get(x - 1, y)
    if charLeft and unicode.isWide(charLeft) then
      -- The char left to the specified position is a wide char.
      return true, true
    end
  end
  -- Not a wide char.
  return false
end

function methods:isAvailable()
  if getScreenAddress(self) then
    return true
  end
  return false
end

function methods:focus()
  setFocus(self, getScreenAddress(self))
end

function methods:unfocus()
  local screenAddress = term.__usedScreen[self]
  if screenAddress then
    if term.__focus[screenAddress] == self or term.__nextFocus[screenAddress] == self then
      term.__focus[screenAddress] = nil
      term.__nextFocus[screenAddress] = nil
      self:focusNext()
    end
  end
end

function methods:focusNext()
  local state = self.__state
  local i, n = 1, #state.neighbors
  if n > 1 then
    for i = 1, n do
      if state.neighbors[i] == self then
        state.neighbors[(i % n) + 1]:focus()
        break
      end
    end
  end
end

function methods:focusPrevious()
  local state = self.__state
  local i, n = 1, #state.neighbors
  if n > 1 then
    for i = 1, n do
      if state.neighbors[i] == self then
        state.neighbors[((i - 2) % n) + 1]:focus()
        break
      end
    end
  end
end

function methods:hasFocus(screenAddress)
  checkArg(1, screenAddress, "string", "nil")
  screenAddress = screenAddress or getScreenAddress(self)
  local keyboardAddress
  if screenAddress then
    keyboardAddress = term.__screenToKeyboard[screenAddress]
    if not next(term.__focus) then
      term.__focus[screenAddress] = self
    end
    if term.__focus[screenAddress] == self then
      return true, screenAddress, keyboardAddress
    end
  end
  return false, screenAddress, keyboardAddress
end

function methods:read(history, dobreak, hint, pwchar, filter)
  checkArg(1, history, "table", "nil")
  checkArg(3, hint, "function", "table", "nil")
  checkArg(4, pwchar, "string", "nil")
  checkArg(5, filter, "string", "function", "nil")
  history = history or {}
  table.insert(history, "")

  local state = self.__state
  local offset = self:getCursor() - 1
  local scrollX = 0
  local textCursorX = 1
  local historyIndex = #history

  if type(hint) == "table" then
    local hintTable = hint
    hint = function()
      return hintTable
    end
  end
  local hintCache, hintIndex
  local redraw

  if pwchar and unicode.len(pwchar) > 0 then
    pwchar = unicode.sub(pwchar, 1, 1)
  end

  if type(filter) == "string" then
    local pattern = filter
    filter = function(line)
      return line:match(pattern)
    end
  end

  local function masktext(str)
    return pwchar and pwchar:rep(unicode.len(str)) or str
  end

  local function getTextCursor()
    return textCursorX
  end
  
  local function getHistoryIndex()
    return historyIndex
  end
  
  local function setHistoryIndex(newIndex)
    historyIndex = newIndex
  end

  local function line()
    return history[getHistoryIndex()]
  end

  local function clearHint()
    hintCache = nil
  end

  local function setTextCursor(newCursor)
    local x, y, w, h = self:getGlobalArea()
    local termCursorX, termCursorY = self:getCursor()
    local str = line() .. " "
    
    textCursorX = math.max(1, math.min(unicode.len(str), newCursor))
    
    while offset + unicode.wlen(unicode.sub(str, 1 + scrollX, textCursorX)) > w do
      scrollX = scrollX + 1
    end
    if textCursorX <= scrollX then
      scrollX = textCursorX - 1
    end
    termCursorX = offset + unicode.wlen(unicode.sub(str, 1 + scrollX, textCursorX - 1)) + 1
    
    self:setCursor(termCursorX, termCursorY)
    redraw()
    clearHint()
  end

  local function copyIfNecessary()
    local index = getHistoryIndex()
    if index ~= #history then
      history[#history] = line()
      setHistoryIndex(#history)
    end
  end

  redraw = function()
    local _, termCursorY = self:getCursor()
    local x, y, w, h = self:getGlobalArea()
    local l = w - offset
    local str = history[getHistoryIndex()]
    str = masktext(unicode.sub(str, scrollX + 1, scrollX + l))
    str = (unicode.wlen(str) > l) and unicode.wtrunc(str, l + 1) or str
    str = text.padRight(str, l)
    gpu(self).set(x + offset, termCursorY + y - 1, str)
  end

  local function home()
    setTextCursor(1)
  end

  local function ende()
    setTextCursor(unicode.len(line()) + 1)
  end

  local function left()
    local cursorX = getTextCursor()
    if cursorX > 1 then
      setTextCursor(cursorX - 1)
      return true -- for backspace
    end
  end

  local function right(n)
    n = n or 1
    local cursorX = getTextCursor()
    local maxX = unicode.len(line()) + 1
    if cursorX < maxX then
      setTextCursor(math.min(maxX, cursorX + n))
    end
  end

  local function up()
    local index = getHistoryIndex()
    if index > 1 then
      setHistoryIndex(index - 1)
      redraw()
      ende()
    end
  end

  local function down()
    local index = getHistoryIndex()
    if index < #history then
      setHistoryIndex(index + 1)
      redraw()
      ende()
    end
  end

  local function delete()
    copyIfNecessary()
    clearHint()
    local cursorX = getTextCursor()
    local index = getHistoryIndex()
    if cursorX <= unicode.len(line()) then
      history[index] = unicode.sub(line(), 1, cursorX - 1) ..
                       unicode.sub(line(), cursorX + 1)
      redraw()
    end
  end

  local function insert(value)
    copyIfNecessary()
    clearHint()
    local cursorX = getTextCursor()
    local index = getHistoryIndex()
    history[index] = unicode.sub(line(), 1, cursorX - 1) ..
                     value ..
                     unicode.sub(line(), cursorX)
    right(unicode.len(value))
  end

  local function tab(direction)
    local cursorX = getTextCursor()
    local historyIndex = getHistoryIndex()
    if not hintCache then -- hint is never nil, see onKeyDown
      local full_line = line()
      hintCache = hint(full_line, cursorX)
      hintIndex = 0
      if type(hintCache) == "string" then
        hintCache = {hintCache}
      end
      if type(hintCache) ~= "table" or #hintCache < 1 then
        hintCache = nil -- invalid hint
      else
        hintCache.trail = full_line:len() - cursorX -- zero when at end of line
      end
    end
    if hintCache then
      hintIndex = (hintIndex + direction + #hintCache - 1) % #hintCache + 1
      history[historyIndex] = tostring(hintCache[hintIndex])
      -- because all other cases of the cursor being moved will result
      -- in the hint cache getting invalidated we do that in setCursor,
      -- so we have to back it up here to restore it after moving.
      local savedCache = hintCache
      redraw()
      ende()
      setTextCursor(getTextCursor()-savedCache.trail-1)
      if #savedCache > 1 then -- stop if only one hint exists.
        hintCache = savedCache
      end
    end
  end

  local function onKeyDown(keyboardAddress, char, code)
    self:setCursorBlink(false)
    if code == keyboard.keys.back then
      if left() then delete() end
    elseif code == keyboard.keys.delete then
      delete()
    elseif code == keyboard.keys.left then
      left()
    elseif code == keyboard.keys.right then
      right()
    elseif code == keyboard.keys.home then
      home()
    elseif code == keyboard.keys["end"] then
      ende()
    elseif code == keyboard.keys.up then
      up()
    elseif code == keyboard.keys.down then
      down()
    elseif code == keyboard.keys.tab then
      if not keyboard.isControlDown(keyboardAddress) and hint then
        tab(keyboard.isShiftDown(keyboardAddress) and -1 or 1)
      end
    elseif code == keyboard.keys.enter then
      if not filter or filter(line() or "") then
        local index = getHistoryIndex()
        if index ~= #history then -- bring entry to front
          history[#history] = line()
          table.remove(history, index)
        end
        return true, history[#history] .. "\n"
      else
        computer.beep(2000, 0.1)
      end
    elseif keyboard.isControlDown(keyboardAddress) and code == keyboard.keys.d then
      if line() == "" then
        history[#history] = ""
        return true, nil
      end
    elseif not keyboard.isControl(char) then
      insert(unicode.char(char))
    end
    self:setCursorBlink(true)
    self:setCursorBlink(true) -- force toggle to caret
  end

  local function onClipboard(value)
    copyIfNecessary()
    self:setCursorBlink(false)
    local cursorX = getTextCursor()
    local index = getHistoryIndex()
    local l = value:find("\n", 1, true)
    if l then
      history[index] = unicode.sub(line(), 1, cursorX - 1)
      redraw()
      insert(unicode.sub(value, 1, l - 1))
      return true, line() .. "\n"
    else
      insert(value)
      self:setCursorBlink(true)
      self:setCursorBlink(true) -- force toggle to caret
    end
  end
  
  local function onTouch(x, y)
    local xWin, yWin, wWin, hWin = self:getGlobalArea()
    x = x - xWin + 1
    y = y - yWin + 1
    if x > 0 and y > 0 and x <= wWin and y <= hWin then
      local _, termCursorY = self:getCursor()
      if y == termCursorY and x > offset then
        local l = wWin - offset
        local str = history[getHistoryIndex()]
        str = masktext(unicode.sub(str, scrollX + 1, scrollX + l))
        str = (unicode.wlen(str) >= x - offset) and unicode.wtrunc(str, x - offset) or str
        setTextCursor(scrollX + 1 + unicode.len(str))
      end
    end
  end

  local function cleanup()
    if history[#history] == "" then
      table.remove(history)
    end
    self:setCursorBlink(false)
    if self:getCursor() > 1 and dobreak ~= false then
      self:write("\n")
    end
  end

  self:setCursorBlink(true)
  while self:isAvailable() do
    local ok, name, address, charOrValueOrX, codeOrY = pcall(event.pull)
    if not ok then
      cleanup()
      error("interrupted", 0)
    end
    if name == "interrupted" then
      cleanup()
      return nil
    end
    local isInFocus, screenAddress, keyboardAddress = self:hasFocus()
    --screen may have changed since pull
    if isInFocus and type(address) == "string" then
      local done, result
      if address == screenAddress then
        if name == "touch" then
          done, result = onTouch(charOrValueOrX, codeOrY)
        end
      elseif address == keyboardAddress then
        if isInFocus then
          if name == "key_down" then
            done, result = onKeyDown(address, charOrValueOrX, codeOrY)
          elseif name == "clipboard" then
            done, result = onClipboard(charOrValueOrX)
          end
        end
      end

      if done then
        cleanup()
        return result
      end
    end
  end
  cleanup()
  return nil -- fail the read if term becomes unavailable
end

function methods:write(value, wrap)
  local stdout = io.output()
  local stream = stdout and stdout.stream
  if stream then
    local previous_wrap = stream.wrap
    stream.wrap = wrap == nil and true or wrap
    stdout:write(value)
    stdout:flush()
    stream.wrap = previous_wrap
  end
end

function methods:debug(...)
  local args = {...}
  for _,v in ipairs(args) do
    local s = v
    if type(s) ~= 'string' then
      local r = require('serialization').serialize(s,10000)
      if type(s) == 'thread' then
        r = string.format('%s{%s}',r,coroutine.status(s))
      end
      s = r
    end
    self:drawText(s,true)
    if _ < #args then
      self:drawText('\t',true)
    end
  end
  self:drawText('\n',true)
end

function methods:drawText(value, wrap)
  if not self:isAvailable() then
    return
  end
  value = tostring(value):gsub("\0", "")
  value = text.detab(value)
  if unicode.wlen(value) == 0 then
    return
  end
  do
    local noBell = value:gsub("\a", "")
    if #noBell ~= #value then
      value = noBell
      computer.beep()
    end
  end
  local x, y, w, h = self:getGlobalArea()
  if not w then
    return -- gpu lost its screen but the signal wasn't processed yet.
  end
  local blink = self:getCursorBlink()
  local state = self.__state
  self:setCursorBlink(false)
  local line, nl
  repeat
    local remainingWidth = w - (state.cursorX - 1)
    local wrapAfter, margin = math.huge, math.huge
    if wrap then
      wrapAfter, margin = remainingWidth, w
    end
    line, value, nl = text.wrap(value, wrapAfter, margin)
    line = (unicode.wlen(line) > remainingWidth) and unicode.wtrunc(line, remainingWidth + 1) or line
    gpu(self).set(state.cursorX + x - 1, state.cursorY + y - 1, line)
    state.cursorX = state.cursorX + unicode.wlen(line)
    if nl or (state.cursorX > w and wrap) then
      state.cursorX = 1
      state.cursorY = state.cursorY + 1
    end
    if state.cursorY > h then
      gpu(self).copy(x, y + 1, w, h - 1, 0, -1)
      gpu(self).fill(x, h + y - 1, w, 1, " ")
      state.cursorY = h
    end
  until not value
  self:setCursorBlink(blink)
end

-------------------------------------------------------------------------------

--Automaticly generate term library functions from window methods
for k, v in pairs(methods) do
  if type(v) == "function" then
    term[k] = function(...)
      local self = term.getWindow()
      return self[k](self, ...)
    end
  end
end

return term
