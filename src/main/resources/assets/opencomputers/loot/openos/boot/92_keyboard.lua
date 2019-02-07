local event = require("event")
local keyboard = require("keyboard")

local function onKeyChange(ev, _, char, code)
  -- nil might be slightly more mem friendly during runtime
  -- and `or nil` appears to only cost 30 bytes
  keyboard.pressedChars[char] = ev == "key_down" or nil
  keyboard.pressedCodes[code] = ev == "key_down" or nil
end

event.listen("key_down", onKeyChange)
event.listen("key_up", onKeyChange)
