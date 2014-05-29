local component = require("component")
local event = require("event")
local keyboard = require("keyboard")

local function onKeyDown(_, address, char, code)
  if component.isPrimary(address) then
    keyboard.pressedChars[char] = true
    keyboard.pressedCodes[code] = true
  end
end

local function onKeyUp(_, address, char, code)
  if component.isPrimary(address) then
    keyboard.pressedChars[char] = nil
    keyboard.pressedCodes[code] = nil
  end
end

local function onComponentUnavailable(_, componentType)
  if componentType == "keyboard" then
    keyboard.pressedChars = {}
    keyboard.pressedCodes = {}
  end
end

event.listen("key_down", onKeyDown)
event.listen("key_up", onKeyUp)
event.listen("component_unavailable", onComponentUnavailable)
