local component = require("component")
local event = require("event")
local keyboard = require("keyboard")

local function onKeyDown(_, address, char, code)
  if keyboard.pressedChars[address] then
    keyboard.pressedChars[address][char] = true
    keyboard.pressedCodes[address][code] = true
  end
end

local function onKeyUp(_, address, char, code)
  if keyboard.pressedChars[address] then
    keyboard.pressedChars[address][char] = nil
    keyboard.pressedCodes[address][code] = nil
  end
end

local function onComponentAdded(_, address, componentType)
  if componentType == "keyboard" then
    keyboard.pressedChars[address] = {}
    keyboard.pressedCodes[address] = {}
  end
end

local function onComponentRemoved(_, address, componentType)
  if componentType == "keyboard" then
    keyboard.pressedChars[address] = nil
    keyboard.pressedCodes[address] = nil
  end
end

for address in component.list("keyboard", true) do
  onComponentAdded("component_added", address, "keyboard")
end

event.listen("key_down", onKeyDown)
event.listen("key_up", onKeyUp)
event.listen("component_added", onComponentAdded)
event.listen("component_removed", onComponentRemoved)
