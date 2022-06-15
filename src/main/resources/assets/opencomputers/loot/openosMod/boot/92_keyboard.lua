local event = require("event")
local keyboard = require("keyboard")
local term = require("term")
local su = require("superUtiles")

local function onKeyChange(ev, uuid, char, code)
  -- nil might be slightly more mem friendly during runtime
  -- and `or nil` appears to only cost 30 bytes
  if su.inTable(term.keyboards(), uuid) then
    keyboard.pressedChars[char] = ev == "key_down" or nil
    keyboard.pressedCodes[code] = ev == "key_down" or nil
  end
end

event.listen("key_down", onKeyChange)
event.listen("key_up", onKeyChange)