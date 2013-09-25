--[[ Top level program run by the kernel.

  We actually do quite a bit of work here, since we want to provide at least
  some very rudimentary way to print to screens - flying blind really would
  be a bit too harsh. And to get that in a robust fashion we also want to
  keep track of connected components. For which we want to keep track of
  signals related to that.

  Thus we have these basic program parts:
  - Events: provide a global event system into which signals are injected as
      they come in in a global event loop, or a convenience `coroutine.sleep`
      function.
  - Components: keeps track of components via an ID unique for this computer,
      which will still be valid if a component changes its address.
  - Terminal: basic implementation of a write function that keeps track of
      the first connected GPU and Screen and an internal cursor position. It
      will provide a global `write` function and provides wrapping + scrolling.
  - Command line: simple command line that allows entering a single line
      command that will be executed when pressing enter.
]]

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

--[[ Event API table. ]]
event = {}

--[[ Register a new event listener for the specified event. ]]
function event.listen(name, callback, weak)
  checkArg(2, callback, "function")
  listenersFor(name, weak)[callback] = true
end

--[[ Remove an event listener. ]]
function event.ignore(name, callback)
  listenersFor(name, false)[callback] = nil
  listenersFor(name, true)[callback] = nil
end

--[[ Dispatch an event with the specified parameter. ]]
function event.fire(name, ...)
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
    event.fire(os.signal(nil, target - os.clock()))
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

event.listen("component_added", function(_, address)
  local id = #components + 1
  components[id] = {address = address, name = driver.componentType(address)}
  event.fire("component_installed", id)
end)

event.listen("component_removed", function(_, address)
  local id = component.id(address)
  components[id] = nil
  event.fire("component_uninstalled", id)
end)

event.listen("component_changed", function(_, newAddress, oldAddress)
  components[component.id(oldAddress)].address = newAddress
end)


--[[ Setup terminal API. ]]
local idGpu, idScreen = 0, 0
local boundGpu = nil
local cursorX, cursorY = 1, 1

event.listen("component_installed", function(_, id)
  local type = component.type(id)
  if type == "gpu" and idGpu < 1 then
    term.gpuId(id)
  elseif type == "screen" and idScreen < 1 then
    term.screenId(id)
  end
end)

event.listen("component_uninstalled", function(_, id)
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
      event.fire("term_available")
    end
  elseif boundGpu then
    boundGpu = nil
    event.fire("term_unavailable")
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
  local w, h = gpu.getResolution()
  if w < 1 or h < 1 then return end
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
  checkCursor()
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
      checkCursor()
    end
    if nl:len() == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
end

function term.clear()
  local gpu = term.gpu()
  if not gpu then return end
  local w, h = gpu.getResolution()
  gpu.fill(1, 1, w, h, " ")
  cursorX, cursorY = 1, 1
end

-- Set custom write function to replace the dummy.
write = function(...)
  local args = {...}
  for _, value in ipairs(args) do
    term.write(value, true)
  end
end


--[[ Primitive command line. ]]
local command = ""
local function commandLine(_, char, code)
  local keys = driver.keyboard.keys
  local gpu = term.gpu()
  local x, y = term.getCursor()
  if code == keys.back then
    if command:len() == 0 then return end
    command = command:sub(1, -2)
    term.setCursor(command:len() + 3, y) -- from leading "> "
    gpu.set(x - 1, y, "  ") -- overwrite cursor blink
  elseif code == keys.enter then
    if command:len() == 0 then return end
    print()
    local code, result = load("return " .. command)
    if code then
      local result = {pcall(code)}
      if not result[1] or result[2] ~= nil then
        -- TODO handle multiple results?
        print(result[2])
      end
    else
      print(result)
    end
    command = ""
    write("> ")
  elseif char ~= 0 then
    -- Non-control character, add to command.
    char = string.char(char)
    command = command .. char
    write(char)
  end
end

-- Reset when the term is reset and ignore input while we have no terminal.
event.listen("term_available", function()
  term.clear()
  command = ""
  write("> ")
  event.listen("key_down", commandLine)
end)
event.listen("term_unavailable", function()
  event.ignore("key_down", commandLine)
end)

-- Serves as main event loop while keeping the cursor blinking. As soon as
-- we run a command from the command line this will stop until the process
-- returned, since indirectly it was called via our sleep.
local blinkState = false
while true do
  coroutine.sleep(0.5)
  local gpu = term.gpu()
  if gpu then
    local x, y = term.getCursor()
    if blinkState then
      term.gpu().set(x, y, string.char(219)) -- Solid block.
    else
      term.gpu().set(x, y, " ")
    end
  end
  blinkState = not blinkState
end
