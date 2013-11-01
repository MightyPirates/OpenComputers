local gpuAvailable, screenAvailable = false, false
local cursorX, cursorY = 1, 1
local cursorBlink = nil

local function gpu() return component.primary("gpu") end

local function toggleBlink()
  if term.isAvailable() then
    local alt = gpu().get(cursorX, cursorY)
    if alt ~= cursorBlink.alt then
      gpu().set(cursorX, cursorY, cursorBlink.alt)
      cursorBlink.alt = alt
      cursorBlink.state = not cursorBlink.state
    end
  end
end

-------------------------------------------------------------------------------

term = {}

function term.isAvailable()
  return gpuAvailable and screenAvailable
end

function term.clear()
  if term.isAvailable() then
    local w, h = gpu().getResolution()
    gpu().fill(1, 1, w, h, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if term.isAvailable() then
    local w = gpu().getResolution()
    gpu().fill(1, cursorY, w, 1, " ")
  end
  cursorX = 1
end

function term.cursor(col, row)
  if col and row then
    local w, h = gpu().getResolution()
    cursorX = math.min(math.max(col, 1), w)
    cursorY = math.min(math.max(row, 1), h)
  end
  return cursorX, cursorY
end

function term.cursorBlink(enabled)
  checkArg(1, enabled, "boolean", "nil")
  local function start(alt)
  end
  local function stop()
  end
  if enabled ~= nil then
    if enabled then
      if not cursorBlink then
        cursorBlink = {}
        cursorBlink.id = event.timer(0.5, toggleBlink, math.huge)
        cursorBlink.state = false
        cursorBlink.alt = unicode.char(0x2588) -- solid block
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
  return cursorBlink ~= nil
end

-------------------------------------------------------------------------------

function term.read(history)
  checkArg(1, history, "table", "nil")
  history = history or {}
  table.insert(history, "")
  local current = #history
  local start = term.cursor()
  local cursor, scroll = 1, 0

  local function remove()
    local x = start - 1 + cursor - scroll
    local w = gpu().getResolution()
    gpu().copy(x + 1, cursorY, w - x, 1, -1, 0)
    local cursor = cursor + (w - x)
    local char = unicode.sub(history[current], cursor, cursor)
    if unicode.len(char) == 0 then
      char = " "
    end
    gpu().set(w, cursorY, char)
  end

  local function render()
    local w = gpu().getResolution()
    local str = unicode.sub(history[current], 1 + scroll, 1 + scroll + w - (start - 1))
    str = str .. string.rep(" ", (w - (start - 1)) - unicode.len(str))
    gpu().set(start, cursorY, str)
  end

  local function scrollEnd()
    local w = gpu().getResolution()
    cursor = unicode.len(history[current]) + 1
    scroll = math.max(0, cursor - (w - (start - 1)))
    render()
  end

  local function scrollLeft()
    scroll = scroll - 1
    local w = gpu().getResolution()
    gpu().copy(start, cursorY, w - start - 1, 1, 1, 0)
    local cursor = w - (start - 1) + scroll
    local char = unicode.sub(history[current], cursor, cursor)
    if unicode.len(char) == 0 then
      char = " "
    end
    gpu().set(1, cursorY, char)
  end

  local function scrollRight()
    scroll = scroll + 1
    local w = gpu().getResolution()
    gpu().copy(start + 1, cursorY, w - start, 1, -1, 0)
    local cursor = w - (start - 1) + scroll
    local char = unicode.sub(history[current], cursor, cursor)
    if unicode.len(char) == 0 then
      char = " "
    end
    gpu().set(w, cursorY, char)
  end

  local function update()
    local w = gpu().getResolution()
    local cursor = cursor - 1
    local x = start - 1 + cursor - scroll
    if cursor < unicode.len(history[current]) then
      gpu().copy(x, cursorY, w - x, 1, 1, 0)
    end
    gpu().set(x, cursorY, unicode.sub(history[current], cursor, cursor))
  end

  local function copyIfNecessary()
    if current ~= #history then
      history[#history] = history[current]
      current = #history
    end
  end

  local function updateCursor()
    cursorX = start - 1 + cursor - scroll
    if not term.cursorBlink() then
      term.cursorBlink(true)
    end
  end

  local function onKeyDown(char, code)
    if not term.isAvailable() then return end
    local w = gpu().getResolution()
    local blink = false
    if code == keyboard.keys.back then
      if cursor > 1 then
        term.cursorBlink(false)
        copyIfNecessary()
        history[#history] = unicode.sub(history[#history], 1, cursor - 2) ..
                            unicode.sub(history[#history], cursor)
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
        remove()
      end
    elseif code == keyboard.keys.delete then
      if cursor <= unicode.len(history[current]) then
        term.cursorBlink(false)
        copyIfNecessary()
        history[#history] = unicode.sub(history[#history], 1, cursor - 1) ..
                            unicode.sub(history[#history], cursor + 1)
        remove()
      end
    elseif code == keyboard.keys.left then
      if cursor > 1 then
        term.cursorBlink(false)
        blink = true
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
      end
    elseif code == keyboard.keys.right then
      if cursor < unicode.len(history[current]) + 1 then
        term.cursorBlink(false)
        blink = true
        cursor = cursor + 1
        if cursor - scroll > w - (start - 1) then
          scrollRight()
        end
      end
    elseif code == keyboard.keys.home then
      if cursor > 1 then
        term.cursorBlink(false)
        blink = true
        cursor, scroll = 1, 0
        render()
      end
    elseif code == keyboard.keys["end"] then
      if cursor < unicode.len(history[current]) + 1 then
        term.cursorBlink(false)
        blink = true
        scrollEnd()
      end
    elseif code == keyboard.keys.up then
      if current > 1 then
        term.cursorBlink(false)
        blink = true
        current = current - 1
        scrollEnd()
      end
    elseif code == keyboard.keys.down then
      if current < #history then
        term.cursorBlink(false)
        blink = true
        current = current + 1
        scrollEnd()
      end
    elseif code == keyboard.keys.enter then
      if current ~= #history then -- bring entry to front
        history[#history] = history[current]
        table.remove(history, current)
      end
      return true, history[#history] .. "\n"
    elseif keyboard.isControlDown() then
      if code == keyboard.keys.d then
        if history[current] == "" then
          history[#history] = ""
          return true, nil
        end
      elseif code == keyboard.keys.c then
        history[#history] = ""
        return true, nil
      end
    elseif not keyboard.isControl(char) then
      term.cursorBlink(false)
      copyIfNecessary()
      history[#history] = unicode.sub(history[#history], 1, cursor - 1) ..
                          unicode.char(char) ..
                          unicode.sub(history[#history], cursor)
      cursor = cursor + 1
      update()
      if cursor - scroll > w - (start - 1) then
        scrollRight()
      end
    end
    updateCursor()
    if blink then -- immediately show cursor
      term.cursorBlink(true)
    end
  end

  local function onClipboard(value)
    copyIfNecessary()
    term.cursorBlink(false)
    local l = value:find("\n", 1, true)
    if l then
      history[#history] = history[#history] .. unicode.sub(value, 1, l - 1)
      render()
      return true, history[#history] .. "\n"
    else
      history[#history] = history[#history] .. value
      scrollEnd()
      updateCursor()
    end
  end

  local function cleanup()
    if history[#history] == "" then
      table.remove(history)
    end
    term.cursorBlink(false)
    print()
  end

  term.cursorBlink(true)
  while term.isAvailable() do
    local ok, event, address, charOrValue, code = pcall(event.pull)
    if not ok then
      cleanup()
      error("interrupted", 0)
    end
    if type(address) == "string" and component.isPrimary(address) then
      if event == "key_down" then
        local done, result = onKeyDown(charOrValue, code)
        if done then
          cleanup()
          return result
        end
      elseif event == "clipboard" then
        local done, result = onClipboard(charOrValue)
        if done then
          cleanup()
          return result
        end
      end
    end
  end
  cleanup()
  return nil -- fail the read if term becomes unavailable
end

function term.write(value, wrap)
  value = tostring(value)
  if unicode.len(value) == 0 or not term.isAvailable() then
    return
  end
  local blink = term.cursorBlink()
  term.cursorBlink(false)
  value = value:gsub("\t", "  ")
  local w, h = gpu().getResolution()
  local function checkCursor()
    if cursorX > w then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > h then
      gpu().copy(1, 1, w, h, 0, -1)
      gpu().fill(1, h, w, 1, " ")
      cursorY = h
    end
  end
  for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
    while wrap and unicode.len(line) > w - cursorX + 1 do
      local partial = unicode.sub(line, 1, w - cursorX + 1)
      line = unicode.sub(line, unicode.len(partial) + 1)
      gpu().set(cursorX, cursorY, partial)
      cursorX = cursorX + unicode.len(partial)
      checkCursor()
    end
    if unicode.len(line) > 0 then
      gpu().set(cursorX, cursorY, line)
      cursorX = cursorX + unicode.len(line)
    end
    if unicode.len(nl) == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
  term.cursorBlink(blink)
end

-------------------------------------------------------------------------------

local function onComponentAvailable(_, componentType)
  local wasAvailable = term.isAvailable()
  if componentType == "gpu" then
    gpuAvailable = true
  elseif componentType == "screen" then
    screenAvailable = true
  end
  if not wasAvailable and term.isAvailable() then
    os.pushSignal("term_available")
  end
end

local function onComponentUnavailable(_, componentType)
  local wasAvailable = term.isAvailable()
  if componentType == "gpu" then
    gpuAvailable = false
  elseif componentType == "screen" then
    screenAvailable = false
  end
  if wasAvailable and not term.isAvailable() then
    os.pushSignal("term_unavailable")
  end
end

return function()
  event.listen("component_available", onComponentAvailable)
  event.listen("component_unavailable", onComponentUnavailable)
end
