--[[ Top level program run by the kernel. ]]

--[[ Distribute signals as events. ]]
local listeners = {}
local weakListeners = {}

local function listenersFor(name, weak)
  checkArg(1, name, "string")
  if weak then
    weakListeners[name] = weakListeners[name] or setmetatable({}, {__mode = "k"})
    return weakListeners[name]
  else
    listeners[name] = listeners[name] or {}
    return listeners[name]
  end
end

function os.listen(name, callback, weak)
  checkArg(2, callback, "function")
  listenersFor(name, weak)[callback] = true
end

function os.ignore(name, callback)
  listenersFor(name, false)[callback] = nil
  listenersFor(name, true)[callback] = nil
end

function os.event(name, ...)
  if name then
    for callback, _ in pairs(listenersFor(name, false)) do
      callback(name, ...)
    end
    for callback, _ in pairs(listenersFor(name, true)) do
      callback(name, ...)
    end
  end
end


--[[ Suspends a thread for the specified amount of time. ]]
function coroutine.sleep(seconds)
  checkArg(1, seconds, "number")
  local target = os.clock() + seconds
  repeat
    os.event(os.signal(nil, target - os.clock()))
  until os.clock() >= target
end


--[[ Keep track of connected components across address changes. ]]
local components = {}
component = {}

function component.address(id)
  local component = components[id]
  if component then
    return component.address
  end
end

function component.type(id)
  local component = components[id]
  if component then
    return component.name
  end
end

function component.id(address)
  for id, component in ipairs(components) do
    if component.address == address then
      return id
    end
  end
end

os.listen("component_added", function(_, address)
  local id = #components + 1
  components[id] = {address = address, name = driver.componentType(address)}
  os.event("component_installed", id)
end)

os.listen("component_removed", function(_, address)
  local id = component.id(address)
  components[id] = nil
  os.event("component_uninstalled", id)
end)

os.listen("component_changed", function(_, newAddress, oldAddress)
  components[component.id(oldAddress)].address = newAddress
end)


--[[ Setup terminal API. ]]
local idGpu, idScreen = 0, 0
local boundGpu = nil
local cursorX, cursorY = 1, 1

os.listen("component_installed", function(_, id)
  local type = component.type(id)
  if type == "gpu" and idGpu < 1 then
    term.gpuId(id)
  elseif type == "screen" and idScreen < 1 then
    term.screenId(id)
  end
end)

os.listen("component_uninstalled", function(_, id)
  if idGpu == id then
    term.gpuId(0)
  elseif idScreen == id then
    term.screenId(0)
  end
end)

term = {}

function term.gpu()
  return boundGpu
end


local function bindIfPossible()
  if idGpu > 0 and idScreen > 0 then
    if not boundGpu then
      local function gpu() return component.address(idGpu) end
      local function screen() return component.address(idScreen) end
      boundGpu = driver.gpu.bind(gpu, screen)
    end
  else
    boundGpu = nil
  end
end

function term.gpuId(id)
  if id then
    checkArg(1, id, "number")
    idGpu = id
    bindIfPossible()
  end
  return idGpu
end

function term.screenId(id)
  if id then
    checkArg(1, id, "number")
    idScreen = id
    bindIfPossible()
  end
  return idScreen
end

function term.getCursor()
  return cursorX, cursorY
end

function term.setCursor(col, row)
  checkArg(1, col, "number")
  checkArg(2, row, "number")
  cursorX = math.max(col, 1)
  cursorY = math.max(row, 1)
end

function term.write(value, wrap)
  value = tostring(value)
  local gpu = term.gpu()
  if not gpu or value:len() == 0 then return end
  local resX, resY = gpu.getResolution()
  if resX < 1 or resY < 1 then return end
  local function checkCursor()
    if cursorX > resX then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > resY then
      gpu.copy(1, 1, resX, resY, 0, -1)
      gpu.fill(1, resY, resX, 1, " ")
      cursorY = resY
    end
  end
  checkCursor()
  for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
    while wrap and line:len() > resX - cursorX + 1 do
      local partial = line:sub(1, resX - cursorX + 1)
      line = line:sub(partial:len() + 1)
      gpu.set(cursorX, cursorY, partial)
      cursorX = cursorX + partial:len()
      checkCursor()
    end
    if line:len() > 0 then
      gpu.set(cursorX, cursorY, line)
      cursorX = cursorX + line:len()
      checkCursor()
    end
    if nl:len() == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
end

-- Set custom write function to replace the dummy.
write = function(...)
  local args = {...}
  for _, value in ipairs(args) do
    term.write(value, true)
  end
end




os.listen("key_down", function(_, char, code)
  local keys = driver.keyboard.keys
  local gpu = term.gpu()
  local x, y = term.getCursor()
  if code == keys.back then
    term.setCursor(x - 1, y)
    gpu.set(x - 1, y, " ")
  elseif code == keys.delete then
    gpu.set(x, y, " ")
  elseif code == keys.up then
    term.setCursor(x, y - 1)
  elseif code == keys.down then
    term.setCursor(x, y + 1)
  elseif code == keys.left then
    term.setCursor(x - 1, y)
  elseif code == keys.right then
    term.setCursor(x + 1, y)
  elseif code == keys.home then
    term.setCursor(1, y)
  elseif code == keys["end"] then
    local rx, ry = gpu.getResolution()
    term.setCursor(rx, y)
  elseif code == keys.tab then
    write("  ")
  elseif code == keys.enter then
    term.setCursor(1, y + 1)
  elseif char ~= 0 then
    write(string.char(char))
  end
end)



while true do
  os.event(os.signal())
end