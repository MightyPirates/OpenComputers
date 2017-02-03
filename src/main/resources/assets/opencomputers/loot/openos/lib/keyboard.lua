local component = require("component")

local keyboard = {pressedChars = {}, pressedCodes = {}}

-- these key definitions are only a subset of all the defined keys
-- __index loads all key data from /lib/tools/keyboard_full.lua (only once)
-- new key metadata should be added here if required for boot
keyboard.keys = {
  c               = 0x2E,
  d               = 0x20,
  q               = 0x10,
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
}

-- Create inverse mapping for name lookup.
setmetatable(keyboard.keys,
{
  __index = function(tbl, k)
    getmetatable(keyboard.keys).__index = nil -- to be safe
    loadfile(package.searchpath("tools/keyboard_full", package.path), "t", setmetatable({keyboard=keyboard},{__index=_G}))()
    return tbl[k]
  end
})

-------------------------------------------------------------------------------

local function getKeyboardAddress(address)
  return address or require("term").keyboard()
end

local function getPressedCodes(address)
  address = getKeyboardAddress(address)
  return address and keyboard.pressedCodes[address] or false
end

local function getPressedChars(address)
  address = getKeyboardAddress(address)
  return address and keyboard.pressedChars[address] or false
end

function keyboard.isAltDown(address)
  checkArg(1, address, "string", "nil")
  local pressedCodes = getPressedCodes(address)
  return pressedCodes and (pressedCodes[keyboard.keys.lmenu] or pressedCodes[keyboard.keys.rmenu]) ~= nil
end

function keyboard.isControl(char)
  return type(char) == "number" and (char < 0x20 or (char >= 0x7F and char <= 0x9F))
end

function keyboard.isControlDown(address)
  checkArg(1, address, "string", "nil")
  local pressedCodes = getPressedCodes(address)
  return pressedCodes and (pressedCodes[keyboard.keys.lcontrol] or pressedCodes[keyboard.keys.rcontrol]) ~= nil
end

function keyboard.isKeyDown(charOrCode, address)
  checkArg(1, charOrCode, "string", "number")
  checkArg(2, address, "string", "nil")
  if type(charOrCode) == "string" then
    local pressedChars = getPressedChars(address)
    return pressedChars and pressedChars[utf8 and utf8.codepoint(charOrCode) or charOrCode:byte()]
  elseif type(charOrCode) == "number" then
    local pressedCodes = getPressedCodes(address)
    return pressedCodes and pressedCodes[charOrCode]
  end
end

function keyboard.isShiftDown(address)
  checkArg(1, address, "string", "nil")
  local pressedCodes = getPressedCodes(address)
  return pressedCodes and (pressedCodes[keyboard.keys.lshift] or pressedCodes[keyboard.keys.rshift]) ~= nil
end

-------------------------------------------------------------------------------

return keyboard
