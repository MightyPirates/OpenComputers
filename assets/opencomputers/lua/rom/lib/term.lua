local gpuAddress, screenAddress, keyboardAddress = nil, nil, nil
local width, height = 0, 0
local cursorX, cursorY = 1, 1
local cursorBlink = nil

local function rebind(gpu, screen)
  if gpu == gpuAddress and screen == screenAddress then
    return
  end
  local oldGpu, oldScreen = gpuAddress, screenAddress
  gpuAddress, screenAddress = gpu, screen
  if gpu and screen then
    driver.gpu.bind(gpuAddress, screenAddress)
    width, height = driver.gpu.resolution(gpuAddress)
    event.fire("term_available")
  elseif gpuAddress and screenAddress then
    width, height = 0, 0
    event.fire("term_unavailable")
  end
end

local function toggleBlink()
  cursorBlink.state = not cursorBlink.state
  if term.isAvailable() then
    local char = cursorBlink.state and cursorBlink.solid or cursorBlink.alt
    driver.gpu.set(term.gpu(), cursorX, cursorY, char)
  end
end

-------------------------------------------------------------------------------

term = {}

function term.isAvailable()
  return gpuAddress and screenAddress
end

function term.clear()
  if term.isAvailable() then
    driver.gpu.fill(term.gpu(), 1, 1, width, height, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if term.isAvailable() then
    driver.gpu.fill(term.gpu(), 1, cursorY, width, 1, " ")
  end
  cursorX = 1
end

function term.cursor(col, row)
  if col and row then
    cursorX = math.max(col, 1)
    cursorY = math.max(row, 1)
  end
  return cursorX, cursorY
end

function term.cursorBlink(enabled)
  local function start(alt)
    if not cursorBlink then
      cursorBlink = event.interval(0.5, toggleBlink)
      cursorBlink.state = false
      cursorBlink.solid = string.char(0x2588) -- 0x2588 is a solid block.
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
         (not cursorBlink or enabled:sub(1, 1) ~= cursorBlink.alt)
  then
    if enabled:len() > 0 then
      start(enabled:sub(1, 1))
    else
      stop()
    end
  end
  return cursorBlink ~= nil
end

function term.gpu(...)
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(1, args[1], "string", "nil")
    rebind(args[1], term.screen())
  end
  return gpuAddress
end

function term.keyboard(...)
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(1, args[1], "string", "nil")
    keyboardAddress = args[1]
  end
  return keyboardAddress
end

function term.screen(...)
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(1, args[1], "string", "nil")
    rebind(term.gpu(), args[1])
  end
  return screenAddress
end

function term.size()
  return width, height
end

-------------------------------------------------------------------------------

function term.read(history)
  history = history or {}
  table.insert(history, "")
  local current = #history
  local keys = driver.keyboard.keys
  local start, y = term.cursor()
  local cursor, scroll = 1, 0
  local keyRepeat = nil
  local result = nil
  local function remove()
    if term.isAvailable() then
      local x = start - 1 + cursor - scroll
      local w = term.size()
      driver.gpu.copy(term.gpu(), x + 1, y, w - x, 1, -1, 0)
      local cursor = cursor + (w - x)
      local char = history[current]:sub(cursor, cursor)
      if char:len() == 0 then
        char = " "
      end
      driver.gpu.set(term.gpu(), w, y, char)
    end
  end
  local function render()
    if term.isAvailable() then
      local w = term.size()
      local str = history[current]:sub(1 + scroll, 1 + scroll + w - (start - 1))
      str = str .. string.rep(" ", (w - (start - 1)) - str:len())
      driver.gpu.set(term.gpu(), start, y, str)
    end
  end
  local function update()
    if term.isAvailable() then
      local w = term.size()
      local cursor = cursor - 1
      local x = start - 1 + cursor - scroll
      if cursor < history[current]:len() then
        driver.gpu.copy(term.gpu(), x, y, w - x, 1, 1, 0)
      end
      driver.gpu.set(term.gpu(), x, y, history[current]:sub(cursor, cursor))
    end
  end
  local function scrollLeft()
    scroll = scroll - 1
    if term.isAvailable() then
      local w = term.size()
    end
  end
  local function scrollRight()
    scroll = scroll + 1
    if term.isAvailable() then
      local w = term.size()
      driver.gpu.copy(term.gpu(), start + 1, y, w - start, 1, -1, 0)
      local cursor = w - (start - 1) + scroll
      local char = history[current]:sub(cursor, cursor)
      if char:len() == 0 then
        char = " "
      end
      driver.gpu.set(term.gpu(), w, y, char)
    end
  end
  local function scrollEnd()
    local w = term.size()
    cursor = history[current]:len() + 1
    scroll = math.max(0, cursor - (w - (start - 1)))
    render()
  end
  local function copyIfNecessary()
    if current ~= #history then
      history[#history] = history[current]
      current = #history
    end
  end
  local function updateCursor()
    term.cursor(start - 1 + cursor - scroll, y)
    term.cursorBlink(cursor <= history[current]:len() and
                     history[current]:sub(cursor, cursor) or " ")
  end
  local function handleKeyPress(char, code)
    local w, h = term.size()
    local cancel = false
    term.cursorBlink(false)
    if code == keys.back then
      if cursor > 1 then
        copyIfNecessary()
        history[current] = history[current]:sub(1, cursor - 2) ..
                           history[current]:sub(cursor)
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
        remove()
      end
      cancel = cursor == 1
    elseif code == keys.delete then
      if cursor <= history[current]:len() then
        copyIfNecessary()
        history[current] = history[current]:sub(1, cursor - 1) ..
                           history[current]:sub(cursor + 1)
        remove()
      end
      cancel = cursor == history[current]:len() + 1
    elseif code == keys.left then
      if cursor > 1 then
        cursor = cursor - 1
        if cursor - scroll < 1 then
          scrollLeft()
        end
      end
      cancel = cursor == 1
    elseif code == keys.right then
      if cursor < history[current]:len() + 1 then
        cursor = cursor + 1
        if cursor - scroll > w - (start - 1) then
          scrollRight()
        end
      end
      cancel = cursor == history[current]:len() + 1
    elseif code == keys.home then
      if cursor > 1 then
        cursor, scroll = 1, 0
        render()
      end
    elseif code == keys["end"] then
      if cursor < history[current]:len() + 1 then
        scrollEnd()
      end
    elseif code == keys.up then
      if current > 1 then
        current = current - 1
        scrollEnd()
      end
      cancel = current == 1
    elseif code == keys.down then
      if current < #history then
        current = current + 1
        scrollEnd()
      end
      cancel = current == #history
    elseif code == keys.enter then
      if current ~= #history then -- bring entry to front
        history[#history] = history[current]
        table.remove(history, current)
        current = #history
      end
      result = history[current] .. "\n"
      if history[current]:len() == 0 then
        table.remove(history, current)
      end
      return true
    elseif not keys.isControl(char) then
      copyIfNecessary()
      history[current] = history[current]:sub(1, cursor - 1) ..
                         string.char(char) ..
                         history[current]:sub(cursor)
      cursor = cursor + 1
      update()
      if cursor - scroll > w - (start - 1) then
        scrollRight()
      end
    end
    updateCursor()
    return cancel
  end
  local function onKeyDown(_, address, char, code)
    if address ~= term.keyboard() then
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
    if address ~= term.keyboard() then
      return
    end
    if keyRepeat then
      keyRepeat = event.cancel(keyRepeat)
    end
  end
  local function onClipboard(_, address, value)
    if address ~= term.keyboard() then
      return
    end
    copyIfNecessary()
    term.cursorBlink(false)
    local l = value:find("\n", 1, true)
    if l then
      history[current] = history[current] .. value:sub(1, l - 1)
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
  while not result do
    coroutine.sleep()
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
  local w, h = width, height
  if value:len() == 0 or not term.isAvailable() or w < 1 or h < 1 then
    return
  end
  value = value:gsub("\t", "  ")
  local function checkCursor()
    if cursorX > w then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > h then
      driver.gpu.copy(term.gpu(), 1, 1, w, h, 0, -1)
      driver.gpu.fill(term.gpu(), 1, h, w, 1, " ")
      cursorY = h
    end
  end
  for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
    while wrap and line:len() > w - cursorX + 1 do
      local partial = line:sub(1, w - cursorX + 1)
      line = line:sub(partial:len() + 1)
      driver.gpu.set(term.gpu(), cursorX, cursorY, partial)
      cursorX = cursorX + partial:len()
      checkCursor()
    end
    if line:len() > 0 then
      driver.gpu.set(term.gpu(), cursorX, cursorY, line)
      cursorX = cursorX + line:len()
    end
    if nl:len() == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  local type = component.type(address)
  if type == "gpu" and not term.gpu() then
    term.gpu(address)
  elseif type == "screen" and not term.screen() then
    term.screen(address)
  elseif type == "keyboard" and not term.keyboard() then
    term.keyboard(address)
  end
end

local function onComponentRemoved(_, address)
  if term.gpu() == address then
    term.gpu(nil)
    for address in component.list() do
      if component.type(address) == "gpu" then
        term.gpu(address)
        return
      end
    end
  elseif term.screen() == address then
    term.screen(nil)
    for address in component.list() do
      if component.type(address) == "screen" then
        term.screen(address)
        return
      end
    end
  elseif term.keyboard() == address then
    term.keyboard(nil)
    for address in component.list() do
      if component.type(address) == "keyboard" then
        term.keyboard(address)
        return
      end
    end
  end
end

local function onScreenResized(_, address, w, h)
  if term.screen() == address then
    width = w
    height = h
  end
end

function term.install()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
  event.listen("screen_resized", onScreenResized)
end

function term.uninstall()
  event.ignore("component_added", onComponentAdded)
  event.ignore("component_removed", onComponentRemoved)
  event.ignore("screen_resized", onScreenResized)
end
