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

local function checkArg(n, have, ...)
  have = type(have)
  for _, want in pairs({...}) do
    if have == want then return end
  end
  error("bad argument #" .. n .. " (" .. table.concat({...}, " or ") ..
        " expected, got " .. have .. ")", 3)
end

-------------------------------------------------------------------------------

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
local timers = {}

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
  -- We may have no arguments at all if the call is just used to drive the
  -- timer check (for example if we had no signal in coroutine.sleep()).
  if name then
    checkArg(1, name, "string")
    for callback, _ in pairs(listenersFor(name, false)) do
      local result, message = xpcall(callback, event.error, name, ...)
      if not result and message then
        error(message, 0)
      end
    end
    for callback, _ in pairs(listenersFor(name, true)) do
      local result, message = xpcall(callback, event.error, name, ...)
      if not result and message then
        error(message, 0)
      end
    end
  end
  -- Collect elapsed callbacks first, since calling them may in turn lead to
  -- new timers being registered, which would add entries to the table we're
  -- iterating, which is not supported.
  local elapsed = {}
  for id, info in pairs(timers) do
    if info.after < os.clock() then
      table.insert(elapsed, info.callback)
      timers[id] = nil
    end
  end
  for _, callback in ipairs(elapsed) do
    local result, message = xpcall(callback, event.error)
    if not result and message then
      error(message, 0)
    end
  end
end

--[[ Calls the specified function after the specified time. ]]
function event.timed(timeout, callback)
  local id = #timers
  timers[id] = {after = os.clock() + timeout, callback = callback}
  return id
end

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  timers[timerId] = nil
end

--[[ Error handler for ALL event callbacks. If this returns a value,
     the computer will crash. Otherwise it'll keep going. ]]
function event.error(message)
  return message
end

--[[ Suspends a thread for the specified amount of time. ]]
function coroutine.sleep(seconds)
  checkArg(1, seconds, "number")
  local target = os.clock() + seconds
  repeat
    local closest = target
    for _, info in pairs(timers) do
      if info.after < closest then
        closest = info.after
      end
    end
    event.fire(os.signal(nil, closest - os.clock()))
  until os.clock() >= target
end

-------------------------------------------------------------------------------

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
  for id, component in pairs(components) do
    if component.address == address then
      return id
    end
  end
end

function component.ids()
  local id = nil
  return function()
    id = next(components, id)
    return id
  end
end

event.listen("component_added", function(_, address)
  local id = #components + 1
  components[id] = {address = address, name = driver.componentType(address)}
  event.fire("component_installed", id)
end)

event.listen("component_removed", function(_, address)
  local id = component.id(address)
  if id then
    components[id] = nil
    event.fire("component_uninstalled", id)
  end
end)

event.listen("component_changed", function(_, newAddress, oldAddress)
  local id = component.id(oldAddress)
  if oldAddress > 0 and not id then return end
  if oldAddress > 0 and newAddress == 0 then -- ~0 -> 0
    components[id] = nil
    event.fire("component_uninstalled", id)
  elseif oldAddress == 0 and newAddress > 0 then -- 0 -> ~0
    id = #components + 1
    components[id] = {address = newAddress, name = driver.componentType(newAddress)}
    event.fire("component_installed", id)
  elseif oldAddress > 0 and newAddress > 0 then -- ~0 -> ~0
    components[id].address = newAddress
  end
end)

-------------------------------------------------------------------------------

--[[ Setup terminal API. ]]
local gpuId, screenId = 0, 0
local screenWidth, screenHeight = 0, 0
local boundGpu = nil
local cursorX, cursorY = 1, 1

event.listen("component_installed", function(_, id)
  local type = component.type(id)
  if type == "gpu" and gpuId < 1 then
    term.gpuId(id)
  elseif type == "screen" and screenId < 1 then
    term.screenId(id)
  end
end)

event.listen("component_uninstalled", function(_, id)
  if gpuId == id then
    term.gpuId(0)
    for id in component.ids() do
      if component.type(id) == "gpu" then
        term.gpuId(id)
        return
      end
    end
  elseif screenId == id then
    term.screenId(0)
    for id in component.ids() do
      if component.type(id) == "screen" then
        term.screenId(id)
        return
      end
    end
  end
end)

event.listen("screen_resized", function(_, address, w, h)
  local id = component.id(address)
  if id == screenId then
    screenWidth = w
    screenHeight = h
  end
end)

local function bindIfPossible()
  if gpuId > 0 and screenId > 0 then
    if not boundGpu then
      boundGpu = driver.gpu.bind(gpuId, screenId)
      screenWidth, screenHeight = boundGpu.getResolution()
      event.fire("term_available")
    end
  elseif boundGpu then
    boundGpu = nil
    screenWidth, screenHeight = 0, 0
    event.fire("term_unavailable")
  end
end

term = {}

function term.gpu()
  return boundGpu
end

function term.screenSize()
  return screenWidth, screenHeight
end

function term.gpuId(id)
  if id then
    checkArg(1, id, "number")
    gpuId = id
    bindIfPossible()
  end
  return gpuId
end

function term.screenId(id)
  if id then
    checkArg(1, id, "number")
    screenId = id
    bindIfPossible()
  end
  return screenId
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
  local w, h = screenWidth, screenHeight
  if value:len() == 0 or not boundGpu or w < 1 or h < 1 then
    return
  end
  local function checkCursor()
    if cursorX > w then
      cursorX = 1
      cursorY = cursorY + 1
    end
    if cursorY > h then
      boundGpu.copy(1, 1, w, h, 0, -1)
      boundGpu.fill(1, h, w, 1, " ")
      cursorY = h
    end
  end
  for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
    while wrap and line:len() > w - cursorX + 1 do
      local partial = line:sub(1, w - cursorX + 1)
      line = line:sub(partial:len() + 1)
      boundGpu.set(cursorX, cursorY, partial)
      cursorX = cursorX + partial:len()
      checkCursor()
    end
    if line:len() > 0 then
      boundGpu.set(cursorX, cursorY, line)
      cursorX = cursorX + line:len()
    end
    if nl:len() == 1 then
      cursorX = 1
      cursorY = cursorY + 1
      checkCursor()
    end
  end
end

function term.clear()
  if not boundGpu then return end
  boundGpu.fill(1, 1, screenWidth, screenHeight, " ")
  cursorX, cursorY = 1, 1
end

function term.clearLine()
  if not boundGpu then return end
  boundGpu.fill(1, cursorY, screenWidth, 1, " ")
  cursorX = 1
end

-- Set custom write function to replace the dummy.
write = function(...)
  local args = {...}
  local first = true
  for i = 1, #args do
    if not first then
      term.write(", ")
    end
    first = false
    term.write(args[i], true)
  end
end

-------------------------------------------------------------------------------

--[[ Primitive command line. ]]
local keyboardId = 0
local lastCommand, command = "", ""
local isRunning = false

event.listen("component_installed", function(_, id)
  local type = component.type(id)
  if type == "keyboard" and keyboardId < 1 then
    keyboardId = id
  end
end)

event.listen("component_uninstalled", function(_, id)
  if keyboardId == id then
    keyboardId = 0
    for id in component.ids() do
      if component.type(id) == "keyboard" then
        keyboardId = id
        return
      end
    end
  end
end)

-- Put this into the term table since other programs may want to use it, too.
function term.keyboardId(id)
  if id then
    checkArg(1, id, "number")
    keyboardId = id
  end
  return keyboardId
end

local function onKeyDown(_, address, char, code)
  if isRunning then return end -- ignore events while running a command
  if component.id(address) ~= keyboardId then return end
  if not boundGpu then return end
  local x, y = term.getCursor()
  local keys = driver.keyboard.keys
  if code == keys.back then
    if command:len() == 0 then return end
    command = command:sub(1, -2)
    term.setCursor(command:len() + 3, y) -- from leading "> "
    boundGpu.set(x - 1, y, "  ") -- overwrite cursor blink
  elseif code == keys.enter then
    if command:len() == 0 then return end
    print(" ") -- overwrite cursor blink
    local code, result = load("return " .. command, "=stdin")
    if not code then
      code, result = load(command, "=stdin") -- maybe it's a statement
    end
    if code then
      isRunning = true
      local result = {pcall(code)}
      isRunning = false
      if not result[1] or result[2] ~= nil then
        print(table.unpack(result, 2))
      end
    else
      print(result)
    end
    lastCommand = command
    command = ""
    write("> ")
  elseif code == keys.up then
    command = lastCommand
    boundGpu.fill(3, cursorY, screenWidth, 1, " ")
    cursorX = 3
    term.write(command)
    term.setCursor(command:len() + 3, y)
  elseif not keys.isControl(char) then
    -- Non-control character, add to command.
    char = string.char(char)
    command = command .. char
    term.write(char)
  end
end

local function onClipboard(_, address, value)
  if isRunning then return end -- ignore events while running a command
  if component.id(address) ~= keyboardId then return end
  value = value:match("([^\r\n]+)")
  if value and value:len() > 0 then
    command = command .. value
    term.write(value)
  end
end

-- Reset when the term is reset and ignore input while we have no terminal.
event.listen("term_available", function()
  term.clear()
  command = ""
  print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  write("> ")
  event.listen("key_down", onKeyDown)
  event.listen("clipboard", onClipboard)
end)
event.listen("term_unavailable", function()
  event.ignore("key_down", onKeyDown)
  event.ignore("clipboard", onClipboard)
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
      term.gpu().set(x, y, string.char(0x2588)) -- Solid block.
    else
      term.gpu().set(x, y, " ")
    end
  end
  blinkState = not blinkState
end
