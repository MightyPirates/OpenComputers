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

-------------------------------------------------------------------------------

term = {}

function term.available()
  return gpuAddress and screenAddress
end

function term.clear()
  if term.available() then
    driver.gpu.fill(term.gpu(), 1, 1, width, height, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if term.available() then
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
  if type(enabled) == "boolean" and enabled ~= (cursorBlink ~= nil) then
    local function toggleBlink()
      cursorBlink.state = not cursorBlink.state
      if term.available() then
         -- 0x2588 is a solid block.
        local char = cursorBlink.state and string.char(0x2588) or " "
        driver.gpu.set(term.gpu(), cursorX, cursorY, char)
      end
    end
    if enabled then
      cursorBlink = event.interval(0.5, toggleBlink)
      cursorBlink.state = false
    else
      event.cancel(cursorBlink.id)
      if cursorBlink.state then
        toggleBlink()
      end
      cursorBlink = nil
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

function term.write(value, wrap)
  value = tostring(value)
  local w, h = width, height
  if value:len() == 0 or not term.available() or w < 1 or h < 1 then
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

event.listen("component_added", function(_, address)
  local type = component.type(address)
  if type == "gpu" and not term.gpu() then
    term.gpu(address)
  elseif type == "screen" and not term.screen() then
    term.screen(address)
  elseif type == "keyboard" and not term.keyboard() then
    term.keyboard(address)
  end
end)

event.listen("component_removed", function(_, address)
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
end)

event.listen("screen_resized", function(_, address, w, h)
  if term.screen() == address then
    width = w
    height = h
  end
end)