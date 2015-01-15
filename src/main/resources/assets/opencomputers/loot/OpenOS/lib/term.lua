local component = require("component")
local computer = require("computer")
local event = require("event")
local keyboard = require("keyboard")
local text = require("text")
local unicode = require("unicode")

local term = {}
local cursorX, cursorY = 1, 1
local cursorBlink = nil

local function toggleBlink()
  if term.isAvailable() then
    cursorBlink.state = not cursorBlink.state
    if cursorBlink.state then
      cursorBlink.alt = component.gpu.get(cursorX, cursorY) or cursorBlink.alt
      component.gpu.set(cursorX, cursorY, string.rep(unicode.char(0x2588), unicode.charWidth(cursorBlink.alt))) -- solid block
    else
      component.gpu.set(cursorX, cursorY, cursorBlink.alt)
    end
  end
end

-------------------------------------------------------------------------------

function term.clear()
  if term.isAvailable() then
    local w, h = component.gpu.getResolution()
    component.gpu.fill(1, 1, w, h, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if term.isAvailable() then
    local w = component.gpu.getResolution()
    component.gpu.fill(1, cursorY, w, 1, " ")
  end
  cursorX = 1
end

function term.getCursor()
  return cursorX, cursorY
end

function term.setCursor(col, row)
  checkArg(1, col, "number")
  checkArg(2, row, "number")
  if cursorBlink and cursorBlink.state then
    toggleBlink()
  end
  local wide, right = term.isWide(cursorX, cursorY)
  if wide and right then
    cursorX = cursorX - 1
  end
  cursorX = math.floor(col)
  cursorY = math.floor(row)
end

function term.getCursorBlink()
  return cursorBlink ~= nil
end

function term.setCursorBlink(enabled)
  checkArg(1, enabled, "boolean")
  if enabled then
    if not cursorBlink then
      cursorBlink = {}
      cursorBlink.id = event.timer(0.5, toggleBlink, math.huge)
      cursorBlink.state = false
    elseif not cursorBlink.state then
      toggleBlink()
    end
  elseif cursorBlink then
    event.cancel(cursorBlink.id)
    if cursorBlink.state then
      toggleBlink()
    end
    cursorBlink = nil
  end
end

function term.isWide(x, y)
  if not term.isAvailable() then
    return false
  end
  local w, h = component.gpu.getResolution()
  if x < 1 or x > w or y < 1 or y > h then
    return false
  end
  local char = component.gpu.get(x, y)
  if unicode.isWide(char) then
    -- The char at the specified position is a wide char.
    return true
  end
  if char == " " and x > 1 then
    local charLeft = component.gpu.get(x - 1, y)
    if charLeft and unicode.isWide(charLeft) then
      -- The char left to the specified position is a wide char.
      return true, true
    end
  end
  -- Not a wide char.
  return false
end

function term.isAvailable()
  return component.isAvailable("gpu") and component.isAvailable("screen")
end

function term.read(history, dobreak, hint, pwchar, filter)
  checkArg(1, history, "table", "nil")
  checkArg(3, hint, "function", "table", "nil")
  checkArg(4, pwchar, "string", "nil")
  checkArg(5, filter, "string", "function", "nil")
  history = history or {}
  table.insert(history, "")
  local offset = term.getCursor() - 1
  local scrollX, scrollY = 0, #history - 1
  local cursorX = 1

  if type(hint) == "table" then
    local hintTable = hint
    hint = function()
      return hintTable
    end
  end
  local hintCache, hintIndex

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

  local function getCursor()
    return cursorX, 1 + scrollY
  end

  local function line()
    local _, cby = getCursor()
    return history[cby]
  end

  local function clearHint()
    hintCache = nil
  end

  local function setCursor(nbx, nby)
    local w, h = component.gpu.getResolution()
    local cx, cy = term.getCursor()

    scrollY = nby - 1

    nbx = math.max(1, math.min(unicode.len(history[nby]) + 1, nbx))
    local ncx = nbx + offset - scrollX
    if ncx > w then
      local sx = nbx - (w - offset)
      local dx = math.abs(scrollX - sx)
      scrollX = sx
      component.gpu.copy(1 + offset + dx, cy, w - offset - dx, 1, -dx, 0)
      local str = masktext(unicode.sub(history[nby], nbx - (dx - 1), nbx))
      str = text.padRight(str, dx)
      component.gpu.set(1 + math.max(offset, w - dx), cy, unicode.sub(str, 1 + math.max(0, dx - (w - offset))))
    elseif ncx < 1 + offset then
      local sx = nbx - 1
      local dx = math.abs(scrollX - sx)
      scrollX = sx
      component.gpu.copy(1 + offset, cy, w - offset - dx, 1, dx, 0)
      local str = masktext(unicode.sub(history[nby], nbx, nbx + dx))
      --str = text.padRight(str, dx)
      component.gpu.set(1 + offset, cy, str)
    end

    cursorX = nbx
    term.setCursor(nbx - scrollX + offset, cy)
    clearHint()
  end

  local function copyIfNecessary()
    local cbx, cby = getCursor()
    if cby ~= #history then
      history[#history] = line()
      setCursor(cbx, #history)
    end
  end

  local function redraw()
    local cx, cy = term.getCursor()
    local bx, by = 1 + scrollX, 1 + scrollY
    local w, h = component.gpu.getResolution()
    local l = w - offset
    local str = masktext(unicode.sub(history[by], bx, bx + l))
    str = text.padRight(str, l)
    component.gpu.set(1 + offset, cy, str)
  end

  local function home()
    local cbx, cby = getCursor()
    setCursor(1, cby)
  end

  local function ende()
    local cbx, cby = getCursor()
    setCursor(unicode.len(line()) + 1, cby)
  end

  local function left()
    local cbx, cby = getCursor()
    if cbx > 1 then
      setCursor(cbx - 1, cby)
      return true -- for backspace
    end
  end

  local function right(n)
    n = n or 1
    local cbx, cby = getCursor()
    local be = unicode.len(line()) + 1
    if cbx < be then
      setCursor(math.min(be, cbx + n), cby)
    end
  end

  local function up()
    local cbx, cby = getCursor()
    if cby > 1 then
      setCursor(1, cby - 1)
      redraw()
      ende()
    end
  end

  local function down()
    local cbx, cby = getCursor()
    if cby < #history then
      setCursor(1, cby + 1)
      redraw()
      ende()
    end
  end

  local function delete()
    copyIfNecessary()
    clearHint()
    local cbx, cby = getCursor()
    if cbx <= unicode.len(line()) then
      local cw = unicode.charWidth(unicode.sub(line(), cbx))
      history[cby] = unicode.sub(line(), 1, cbx - 1) ..
                     unicode.sub(line(), cbx + 1)
      local cx, cy = term.getCursor()
      local w, h = component.gpu.getResolution()
      component.gpu.copy(cx + cw, cy, w - cx, 1, -cw, 0)
      local br = cbx + (w - cx)
      local char = masktext(unicode.sub(line(), br, br))
      if not char or unicode.wlen(char) == 0 then
        char = " "
      end
      component.gpu.set(w, cy, char)
    end
  end

  local function insert(value)
    copyIfNecessary()
    clearHint()
    local cx, cy = term.getCursor()
    local cbx, cby = getCursor()
    local w, h = component.gpu.getResolution()
    history[cby] = unicode.sub(line(), 1, cbx - 1) ..
                   value ..
                   unicode.sub(line(), cbx)
    local len = unicode.wlen(value)
    local n = w - (cx - 1) - len
    if n > 0 then
      component.gpu.copy(cx, cy, n, 1, len, 0)
    end
    component.gpu.set(cx, cy, masktext(value))
    right(unicode.len(value))
  end

  local function tab(direction)
    local cbx, cby = getCursor()
    if not hintCache then -- hint is never nil, see onKeyDown
      hintCache = hint(line(), cbx)
      hintIndex = 0
      if type(hintCache) == "string" then
        hintCache = {hintCache}
      end
      if type(hintCache) ~= "table" or #hintCache < 1 then
        hintCache = nil -- invalid hint
      end
    end
    if hintCache then
      hintIndex = (hintIndex + direction + #hintCache - 1) % #hintCache + 1
      history[cby] = tostring(hintCache[hintIndex])
      -- because all other cases of the cursor being moved will result
      -- in the hint cache getting invalidated we do that in setCursor,
      -- so we have to back it up here to restore it after moving.
      local savedCache = hintCache
      redraw()
      ende()
      if #savedCache > 1 then -- stop if only one hint exists.
        hintCache = savedCache
      end
    end
  end

  local function onKeyDown(char, code)
    term.setCursorBlink(false)
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
    elseif code == keyboard.keys.tab and hint then
      tab(keyboard.isShiftDown() and -1 or 1)
    elseif code == keyboard.keys.enter then
      if not filter or filter(line() or "") then
        local cbx, cby = getCursor()
        if cby ~= #history then -- bring entry to front
          history[#history] = line()
          table.remove(history, cby)
        end
        return true, history[#history] .. "\n"
      else
        computer.beep(2000, 0.1)
      end
    elseif keyboard.isControlDown() and code == keyboard.keys.d then
      if line() == "" then
        history[#history] = ""
        return true, nil
      end
    elseif keyboard.isControlDown() and code == keyboard.keys.c then
      history[#history] = ""
      return true, nil
    elseif not keyboard.isControl(char) then
      insert(unicode.char(char))
    end
    term.setCursorBlink(true)
    term.setCursorBlink(true) -- force toggle to caret
  end

  local function onClipboard(value)
    copyIfNecessary()
    term.setCursorBlink(false)
    local cbx, cby = getCursor()
    local l = value:find("\n", 1, true)
    if l then
      history[cby] = unicode.sub(line(), 1, cbx - 1)
      redraw()
      insert(unicode.sub(value, 1, l - 1))
      return true, line() .. "\n"
    else
      insert(value)
      term.setCursorBlink(true)
      term.setCursorBlink(true) -- force toggle to caret
    end
  end

  local function cleanup()
    if history[#history] == "" then
      table.remove(history)
    end
    term.setCursorBlink(false)
    if term.getCursor() > 1 and dobreak ~= false then
      print()
    end
  end

  term.setCursorBlink(true)
  while term.isAvailable() do
    local ocx, ocy = getCursor()
    local ok, name, address, charOrValue, code = pcall(event.pull)
    if not ok then
      cleanup()
      error("interrupted", 0)
    end
    local ncx, ncy = getCursor()
    if ocx ~= ncx or ocy ~= ncy then
      cleanup()
      return "" -- soft fail the read if someone messes with the term
    end
    if term.isAvailable() and -- may have changed since pull
       type(address) == "string" and
       component.isPrimary(address)
    then
      local done, result
      if name == "key_down" then
        done, result = onKeyDown(charOrValue, code)
      elseif name == "clipboard" then
        done, result = onClipboard(charOrValue)
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

function term.write(value, wrap)
  if not term.isAvailable() then
    return
  end
  value = text.detab(tostring(value))
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
  local w, h = component.gpu.getResolution()
  if not w then
    return -- gpu lost its screen but the signal wasn't processed yet.
  end
  local blink = term.getCursorBlink()
  term.setCursorBlink(false)
  local line, nl
  repeat
    local wrapAfter, margin = math.huge, math.huge
    if wrap then
      wrapAfter, margin = w - (cursorX - 1), w
    end
    line, value, nl = text.wrap(value, wrapAfter, margin)
    component.gpu.set(cursorX, cursorY, line)
    cursorX = cursorX + unicode.wlen(line)
    if nl or (cursorX > w and wrap) then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > h then
      component.gpu.copy(1, 1, w, h, 0, -1)
      component.gpu.fill(1, h, w, 1, " ")
      cursorY = h
    end
  until not value
  term.setCursorBlink(blink)
end

-------------------------------------------------------------------------------

return term
