local event = require("event")
local keyboard = require("keyboard")

local function onKeyDown(_, _, char, code)
  keyboard.pressedChars[char] = true
  keyboard.pressedCodes[code] = true
end

local function onKeyUp(_, _, char, code)
  keyboard.pressedChars[char] = nil
  keyboard.pressedCodes[code] = nil
end

event.listen("key_down", onKeyDown)
event.listen("key_up", onKeyUp)
