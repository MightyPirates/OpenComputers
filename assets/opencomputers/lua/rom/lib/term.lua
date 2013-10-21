local cursorX, cursorY = 1, 1
local cursorBlink = nil

local function toggleBlink()
  cursorBlink.state = not cursorBlink.state
  if term.isAvailable() then
    local char = cursorBlink.state and cursorBlink.solid or cursorBlink.alt
    gpu.set(cursorX, cursorY, char)
  end
end

-------------------------------------------------------------------------------

term = {}

function term.isAvailable()
  return component.isAvailable("gpu") and component.isAvailable("screen")
end

function term.clear()
  if term.isAvailable() then
    local w, h = gpu.resolution()
    gpu.fill(1, 1, w, h, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if term.isAvailable() then
    local w = gpu.resolution()
    gpu.fill(1, cursorY, w, 1, " ")
  end
  cursorX = 1
end

function term.cursor(col, row)
  if col and row then
    local w, h = gpu.resolution()
    cursorX = math.min(math.max(col, 1), w)
    cursorY = math.min(math.max(row, 1), h)
  end
  return cursorX, cursorY
end

function term.cursorBlink(enabled)
  local function start(alt)
    if not cursorBlink then
      cursorBlink = event.interval(0.5, toggleBlink)
      cursorBlink.state = false
      cursorBlink.solid = string.uchar(0x2588) -- 0x2588 is a solid block.
    elseif cursorBlink.state then
      toggleBlink()
    end
    cursorBlink.alt = alt
  end
  local function stop()
    event.cancel(cursorBlink.id)
    if cursorBlink.state then
      toggleBlink()
    end
    cursorBlink = nil
  end
  if type(enabled) == "boolean" and enabled ~= (cursorBlink ~= nil) then
    if enabled then
      start(" ")
    else
      stop()
    end
  elseif type(enabled) == "string" and
         (not cursorBlink or enabled:usub(1, 1) ~= cursorBlink.alt)
  then
    if enabled:ulen() > 0 then
      start(enabled:usub(1, 1))
    else
      stop()
    end
  end
  return cursorBlink ~= nil
end

-------------------------------------------------------------------------------

function term.read(history)
  history = history or {}
  table.insert(history, "")
  local current = #history
  local keys = driver.keyboard.keys
  local start = term.cursor()
  local cursor, scroll = 1, 0
  local keyRepeat = nil
  local result = nil
  local function remove()
    local x = start - 1 + cursor - scroll
    local _, y = term.cursor()
    local w = gpu.resolution()
    gpu.copy(x + 1, y, w - x, 1, -1, 0)
    local cursor = cursor + (w - x)
    local char = history[current]:usub(cursor, cursor)
    if char:ulen() == 0 then
      char = " "
    end
    gpu.set(w, y, char)
  end
  local function render()
    local _, y = term.cursor()
    local w = gpu.resolution()
    local str = history[current]:usub(1 + scroll, 1 + scroll + w - (start - 1))
    str = str .. string.rep(" ", (w - (start - 1)) - str:ulen())
    gpu.set(start, y, str)
  end
  local function scrollEnd()
    local w = gpu.resolution()
    cursor = history[current]:ulen() + 1
    scroll = math.max(0, cursor - (w - (start - 1)))
    render()
  end
  local function scrollLeft()
    scroll = scroll - 1
    local _, y = term.cursor()
    local w = gpu.resolution()
    gpu.copy(start, y, w - start - 1, 1, 1, 0)
    local cursor = w - (start - 1) + scroll
    local char = history[current]:usub(cursor, cursor)
    if char:ulen() == 0 then
      char = " "
    end
    gpu.set(1, y, char)
  end
  local function scrollRight()
    scroll = scroll + 1
    local _, y = term.cursor()
    local w = gpu.resolution()
    gpu.copy(start + 1, y, w - start, 1, -1, 0)
    local cursor = w - (start - 1) + scroll
    local char = history[current]:usub(cursor, cursor)
    if char:ulen() == 0 then
      char = " "
    end
    gpu.set(w, y, char)
  end
  local function update()
    local _, y = term.cursor()
    local w = gpu.resolution()
    local cursor = cursor - 1
    local x = start - 1 + cursor - scroll
    if cursor < history[current]:ulen() then
      gpu.copy(x, y, w - x, 1, 1, 0)
    end
    gpu.set(x, y, history[current]:usub(cursor, cursor))
  end
  local function copyIfNecessary()
    if current ~= #history then
      history[#history] = history[current]
      current = #history
    end
  end
  local function updateCursor(blink)
    local _, y = term.cursor()
    term.cursor(start - 1 + cursor - scroll, y)
    term.cursorBlink(cursor <= history[current]:ulen() and
                     history[current]:usub(cursor, cursor) or " ")
    if blink then
      toggleBlink()
    end
  end
  local function handleKeyPress(char, code)
    if not term.isAvailable() then return end
    local w, h = gpu.resolution()
    local cancel, blink = false, false
    term.cursorBlink(false)
    if code == keys.back then
      if cursor > 1 then
        copyIfNecessary()
        history[current] = history[current]:usub(1, cursor - 2) ..
                           history[current]:usub(cursor)
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
        remove()
      end
      cancel = cursor == 1
    elseif code == keys.delete then
      if cursor <= history[current]:ulen() then
        copyIfNecessary()
        history[current] = history[current]:usub(1, cursor - 1) ..
                           history[current]:usub(cursor + 1)
        remove()
      end
      cancel = cursor == history[current]:ulen() + 1
    elseif code == keys.left then
      if cursor > 1 then
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
      end
      cancel = cursor == 1
      blink = true
    elseif code == keys.right then
      if cursor < history[current]:ulen() + 1 then
        cursor = cursor + 1
        if cursor - scroll > w - (start - 1) then
          scrollRight()
        end
      end
      cancel = cursor == history[current]:ulen() + 1
      blink = true
    elseif code == keys.home then
      if cursor > 1 then
        cursor, scroll = 1, 0
        render()
      end
      cancel = true
      blink = true
    elseif code == keys["end"] then
      if cursor < history[current]:ulen() + 1 then
        scrollEnd()
      end
      cancel = true
      blink = true
    elseif code == keys.up then
      if current > 1 then
        current = current - 1
        scrollEnd()
      end
      cancel = current == 1
      blink = true
    elseif code == keys.down then
      if current < #history then
        current = current + 1
        scrollEnd()
      end
      cancel = current == #history
      blink = true
    elseif code == keys.enter then
      if current ~= #history then -- bring entry to front
        history[#history] = history[current]
        table.remove(history, current)
        current = #history
      end
      result = history[current] .. "\n"
      if history[current]:ulen() == 0 then
        table.remove(history, current)
      end
      return true
    elseif not keys.isControl(char) then
      copyIfNecessary()
      history[current] = history[current]:usub(1, cursor - 1) ..
                         string.uchar(char) ..
                         history[current]:usub(cursor)
      cursor = cursor + 1
      update()
      if cursor - scroll > w - (start - 1) then
        scrollRight()
      end
    end
    updateCursor(blink)
    return cancel
  end
  local function onKeyDown(_, address, char, code)
    if not component.isAvailable("keyboard") or
       address ~= component.primary("keyboard")
    then
      return
    end
    if keyRepeat then
      keyRepeat = event.cancel(keyRepeat)
    end
    if not handleKeyPress(char, code) then
      local function onRepeatTimer()
        if not handleKeyPress(char, code) then
          keyRepeat = event.timer(0, onRepeatTimer)
        end
      end
      keyRepeat = event.timer(0.4, onRepeatTimer)
    end
  end
  local function onKeyUp(_, address, char, code)
    if not component.isAvailable("keyboard") or
       address ~= component.primary("keyboard")
    then
      return
    end
    if keyRepeat then
      keyRepeat = event.cancel(keyRepeat)
    end
  end
  local function onClipboard(_, address, value)
    if not component.isAvailable("keyboard") or
       address ~= component.primary("keyboard")
    then
      return
    end
    copyIfNecessary()
    term.cursorBlink(false)
    local l = value:find("\n", 1, true)
    if l then
      history[current] = history[current] .. value:usub(1, l - 1)
      result = history[current] .. "\n"
    else
      history[current] = history[current] .. value
      scrollEnd()
      updateCursor()
    end
  end
  event.listen("key_down", onKeyDown)
  event.listen("key_up", onKeyUp)
  event.listen("clipboard", onClipboard)
  term.cursorBlink(true)
  while term.isAvailable() and not result do
    event.wait()
  end
  if history[#history] == "" then
    table.remove(history)
  end
  if keyRepeat then
    event.cancel(keyRepeat)
  end
  event.ignore("key_down", onKeyDown)
  event.ignore("key_up", onKeyUp)
  event.ignore("clipboard", onClipboard)
  term.cursorBlink(false)
  print()
  return result
end

function term.write(value, wrap)
  value = tostring(value)
  if value:ulen() == 0 or not term.isAvailable() then
    return
  end
  value = value:gsub("\t", "  ")
  local w, h = gpu.resolution()
  local function checkCursor()
    if cursorX > w then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > h then
      gpu.copy(1, 1, w, h, 0, -1)
      gpu.fill(1, h, w, 1, " ")
      cursorY = h
    end
  end
  for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
    while wrap and line:ulen() > w - cursorX + 1 do
      local partial = line:usub(1, w - cursorX + 1)
      line = line:usub(partial:ulen() + 1)
      gpu.set(cursorX, cursorY, partial)
      cursorX = cursorX + partial:ulen()
      checkCursor()
    end
    if line:ulen() > 0 then
      gpu.set(cursorX, cursorY, line)
      cursorX = cursorX + line:ulen()
    end
    if nl:ulen() == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
end

-------------------------------------------------------------------------------

local function onComponentAvailable(_, componentType)
  if (componentType == "gpu" and component.isAvailable("screen")) or
     (componentType == "screen" and component.isAvailable("gpu"))
  then
    event.fire("term_available")
  end
end

local function onComponentUnavailable(_, componentType)
  if (componentType == "gpu" and component.isAvailable("screen")) or
     (componentType == "screen" and component.isAvailable("gpu"))
  then
    event.fire("term_unavailable")
  end
end

return function()
  event.listen("component_available", onComponentAvailable)
  event.listen("component_unavailable", onComponentUnavailable)
end
