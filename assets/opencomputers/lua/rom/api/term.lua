local gpu = nil
local gpuAddress, screenAddress, keyboardAddress = false, false, false
local width, height = 0, 0
local cursorX, cursorY = 1, 1
local cursorBlink = nil

local function bindIfPossible()
  if gpuAddress and screenAddress then
    if not gpu then
      gpu = driver.gpu.bind(gpuAddress, screenAddress)
      width, height = gpu.getResolution()
      event.fire("term_available")
    end
  elseif gpu then
    gpu, width, height = nil, 0, 0
    event.fire("term_unavailable")
  end
end

-------------------------------------------------------------------------------

term = {}

function term.clear()
  if gpu then
    gpu.fill(1, 1, width, height, " ")
  end
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if gpu then
    gpu.fill(1, cursorY, width, 1, " ")
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
      if gpu then
         -- 0x2588 is a solid block.
        local char = cursorBlink.state and string.char(0x2588) or " "
        gpu.set(cursorX, cursorY, char)
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

function term.gpu(address)
  if address ~= nil and ({boolean=true, string=true})[type(address)] then
    gpuAddress = address
    bindIfPossible()
  end
  return gpuAddress, gpu
end

function term.keyboard(address)
  if address ~= nil and ({boolean=true, string=true})[type(address)] then
    keyboardAddress = address
  end
  return keyboardAddress
end

function term.screen(address)
  if address ~= nil and ({boolean=true, string=true})[type(address)] then
    screenAddress = address
    bindIfPossible()
  end
  return screenAddress
end

function term.size()
  return width, height
end

function term.write(value, wrap)
  value = tostring(value)
  local w, h = width, height
  if value:len() == 0 or not gpu or w < 1 or h < 1 then
    return
  end
  value = value:gsub("\t", "  ")
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
    while wrap and line:len() > w - cursorX + 1 do
      local partial = line:sub(1, w - cursorX + 1)
      line = line:sub(partial:len() + 1)
      gpu.set(cursorX, cursorY, partial)
      cursorX = cursorX + partial:len()
      checkCursor()
    end
    if line:len() > 0 then
      gpu.set(cursorX, cursorY, line)
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
  if type == "gpu" and not gpuAddress then
    term.gpu(address)
  elseif type == "screen" and not screenAddress then
    term.screen(address)
  elseif type == "keyboard" and not keyboardAddress then
    term.keyboard(address)
  end
end)

event.listen("component_removed", function(_, address)
  if gpuAddress == address then
    term.gpu(false)
    for address in component.list() do
      if component.type(address) == "gpu" then
        term.gpu(address)
        return
      end
    end
  elseif screenAddress == address then
    term.screen(false)
    for address in component.list() do
      if component.type(address) == "screen" then
        term.screen(address)
        return
      end
    end
  elseif keyboardAddress == address then
    term.keyboard(false)
    for address in component.list() do
      if component.type(address) == "keyboard" then
        term.keyboard(address)
        return
      end
    end
  end
end)

event.listen("screen_resized", function(_, address, w, h)
  if address == screenAddress then
    width = w
    height = h
  end
end)