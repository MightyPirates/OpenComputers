local keyboard = {pressedChars = {}, pressedCodes = {}}

-- these key definitions are only a subset of all the defined keys
-- __index loads all key data from /lib/tools/keyboard_full.lua (only once)
-- new key metadata should be added here if required for boot
keyboard.keys = {
  c               = 0x2E,
  d               = 0x20,
  q               = 0x10,
  w               = 0x11,
  back            = 0x0E, -- backspace
  delete          = 0xD3,
  down            = 0xD0,
  enter           = 0x1C,
  home            = 0xC7,
  lcontrol        = 0x1D,
  left            = 0xCB,
  lmenu           = 0x38, -- left Alt
  lshift          = 0x2A,
  pageDown        = 0xD1,
  rcontrol        = 0x9D,
  right           = 0xCD,
  rmenu           = 0xB8, -- right Alt
  rshift          = 0x36,
  space           = 0x39,
  tab             = 0x0F,
  up              = 0xC8,
  ["end"]         = 0xCF,
  enter           = 0x1C,
  tab             = 0x0F,
  numpadenter     = 0x9C,
}

-------------------------------------------------------------------------------

function keyboard.isAltDown()
  return keyboard.pressedCodes[keyboard.keys.lmenu] or keyboard.pressedCodes[keyboard.keys.rmenu]
end

function keyboard.isControl(char)
  return type(char) == "number" and (char < 0x20 or (char >= 0x7F and char <= 0x9F))
end

function keyboard.isControlDown()
  return keyboard.pressedCodes[keyboard.keys.lcontrol] or keyboard.pressedCodes[keyboard.keys.rcontrol]
end

function keyboard.isKeyDown(charOrCode)
  checkArg(1, charOrCode, "string", "number")
  if type(charOrCode) == "string" then
    return keyboard.pressedChars[utf8 and utf8.codepoint(charOrCode) or charOrCode:byte()]
  elseif type(charOrCode) == "number" then
    return keyboard.pressedCodes[charOrCode]
  end
end

function keyboard.isShiftDown()
  return keyboard.pressedCodes[keyboard.keys.lshift] or keyboard.pressedCodes[keyboard.keys.rshift]
end

-------------------------------------------------------------------------------

require("package").delay(keyboard.keys, "/lib/core/full_keyboard.lua")

return keyboard
