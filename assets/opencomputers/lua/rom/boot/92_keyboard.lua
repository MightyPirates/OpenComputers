local event = require("event")

local function onKeyDown(_, address, char, code)
  local component = require("component")
  if component.isPrimary(address) then
    local keyboard = require("keyboard")
    keyboard.pressedChars[char] = true
    keyboard.pressedCodes[code] = true
  end
end

local function onKeyUp(_, address, char, code)
  local component = require("component")
  if component.isPrimary(address) then
    local keyboard = require("keyboard")
    keyboard.pressedChars[char] = nil
    keyboard.pressedCodes[code] = nil
  end
end

local function onComponentUnavailable(_, componentType)
  if componentType == "keyboard" then
    local keyboard = require("keyboard")
    keyboard.pressedChars = {}
    keyboard.pressedCodes = {}
  end
end

event.listen("key_down", onKeyDown)
event.listen("key_up", onKeyUp)
event.listen("component_unavailable", onComponentUnavailable)
