keyboard = setmetatable({}, {__index = driver.keyboard})

local pressedChars = {}
local pressedCodes = {}

-------------------------------------------------------------------------------

function keyboard.isKeyDown(charOrCode)
  checkArg(1, charOrCode, "string", "number")
  if type(charOrCode) == "string" then
    return pressedChars[charOrCode]
  elseif type(charOrCode) == "number" then
    return pressedCodes[charOrCode]
  end
end

function keyboard.isControlDown()
  return (pressedCodes[keyboard.keys.lcontrol] or pressedCodes[keyboard.keys.rcontrol]) ~= nil
end

function keyboard.isShiftDown()
  return (pressedCodes[keyboard.keys.lshift] or pressedCodes[keyboard.keys.rshift]) ~= nil
end

-------------------------------------------------------------------------------

local function onKeyDown(_, address, char, code)
  if component.isPrimary(address) then
    pressedChars[char] = true
    pressedCodes[code] = true
  end
end

local function onKeyUp(_, address, char, code)
  if component.isPrimary(address) then
    pressedChars[char] = nil
    pressedCodes[code] = nil
  end
end

local function onComponentUnavailable(_, componentType)
  if componentType == "keyboard" then
    pressedChars = {}
    pressedCodes = {}
  end
end

return function()
  event.listen("key_down", onKeyDown)
  event.listen("key_up", onKeyUp)
  event.listen("component_unavailable", onComponentUnavailable)
end
