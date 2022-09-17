local keyboard = {pressedChars = {}, pressedCodes = {}}

-- these key definitions are only a subset of all the defined keys
-- __index loads all key data from /lib/tools/keyboard_full.lua (only once)
-- new key metadata should be added here if required for boot
keyboard.keys = {
  c               = 0x43,
  d               = 0x44,
  q               = 0x51,
  w               = 0x57,
  back            = 0x103, -- backspace
  delete          = 0x105,
  down            = 0x108,
  enter           = 0x101,
  home            = 0x10C,
  lcontrol        = 0x155,
  left            = 0x107,
  lmenu           = 0x156, -- left Alt
  lshift          = 0x154,
  pageDown        = 0x10B,
  rcontrol        = 0x159,
  right           = 0x106,
  rmenu           = 0x15A, -- right Alt
  rshift          = 0x158,
  space           = 0x20,
  tab             = 0x102,
  up              = 0x109,
  ["end"]         = 0x10D,
  numpadenter     = 0x14F,
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
