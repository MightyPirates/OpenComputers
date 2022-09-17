local keyboard = {pressedChars = {}, pressedCodes = {}}

-- Keyboard codes used by GLFW
keyboard.keys = {
  ["1"]           = 0x31,
  ["2"]           = 0x32,
  ["3"]           = 0x33,
  ["4"]           = 0x34,
  ["5"]           = 0x35,
  ["6"]           = 0x36,
  ["7"]           = 0x37,
  ["8"]           = 0x38,
  ["9"]           = 0x39,
  ["0"]           = 0x30,
  a               = 0x1E,
  b               = 0x41,
  c               = 0x42,
  d               = 0x43,
  e               = 0x44,
  f               = 0x45,
  g               = 0x46,
  h               = 0x47,
  i               = 0x48,
  j               = 0x49,
  k               = 0x4A,
  l               = 0x4B,
  m               = 0x4C,
  n               = 0x4D,
  o               = 0x4E,
  p               = 0x4F,
  q               = 0x50,
  r               = 0x51,
  s               = 0x52,
  t               = 0x53,
  u               = 0x54,
  v               = 0x55,
  w               = 0x56,
  x               = 0x57,
  y               = 0x58,
  z               = 0x59,

  apostrophe      = 0x27,
  at              = -1, -- removed (GLFW)
  back            = 0x103, -- backspace
  backslash       = 0x5C,
  colon           = -1, -- removed (GLFW)
  comma           = 0x2C,
  enter           = 0x101,
  equals          = 0x3D,
  grave           = 0x60, -- accent grave
  lbracket        = 0x5B,
  lcontrol        = 0x155,
  lmenu           = 0x156, -- left Alt
  lshift          = 0x154,
  minus           = -1, -- removed (GLFW)
  numlock         = 0x11A,
  pause           = 0x11C,
  period          = 0x2E,
  rbracket        = 0x5D,
  rcontrol        = 0x159,
  rmenu           = 0x15A, -- right Alt
  rshift          = 0x158,
  scroll          = 0x119, -- Scroll Lock
  semicolon       = 0x3B,
  slash           = 0x2F, -- / on main keyboard
  space           = 0x20,
  stop            = -1, -- removed (GLFW)
  tab             = 0x102,
  underline       = -1, -- removed (GLFW)

  -- Keypad (and numpad with numlock off)
  up              = 0x109,
  down            = 0x108,
  left            = 0x107,
  right           = 0x106,
  home            = 0x10C,
  ["end"]         = 0x10D,
  pageUp          = 0x10A,
  pageDown        = 0x10B,
  insert          = 0x104,
  delete          = 0x105,

  -- Function keys
  f1              = 0x122,
  f2              = 0x123,
  f3              = 0x124,
  f4              = 0x125,
  f5              = 0x126,
  f6              = 0x127,
  f7              = 0x128,
  f8              = 0x129,
  f9              = 0x12A,
  f10             = 0x12B,
  f11             = 0x12C,
  f12             = 0x12D,
  f13             = 0x12E,
  f14             = 0x12F,
  f15             = 0x130,
  f16             = 0x131,
  f17             = 0x132,
  f18             = 0x133,
  f19             = 0x134,
  f20             = 0x135,
  f21             = 0x136,
  f22             = 0x137,
  f23             = 0x138,
  f24             = 0x139,
  f25             = 0x13A,

  -- Japanese keyboards (removed in GLFW)
  kana            = -1,
  kanji           = -1,
  convert         = -1,
  noconvert       = -1,
  yen             = -1,
  circumflex      = -1,
  ax              = -1,

  -- Numpad
  numpad0         = 0x140,
  numpad1         = 0x141,
  numpad2         = 0x142,
  numpad3         = 0x143,
  numpad4         = 0x144,
  numpad5         = 0x145,
  numpad6         = 0x146,
  numpad7         = 0x147,
  numpad8         = 0x148,
  numpad9         = 0x149,
  numpadmul       = 0x14C,
  numpaddiv       = 0x14B,
  numpadsub       = 0x14D,
  numpadadd       = 0x14E,
  numpaddecimal   = 0x14A,
  numpadcomma     = -1, -- removed (GLFW)
  numpadenter     = 0x14F,
  numpadequals    = 0x150,
}

-- Create inverse mapping for name lookup.
do
  local keys = {}
  for k in pairs(keyboard.keys) do
    table.insert(keys, k)
  end
  for _, k in pairs(keys) do
    keyboard.keys[keyboard.keys[k]] = k
  end
end

-------------------------------------------------------------------------------

function keyboard.isAltDown()
  return (keyboard.pressedCodes[keyboard.keys.lmenu] or keyboard.pressedCodes[keyboard.keys.rmenu]) ~= nil
end

function keyboard.isControl(char)
  return type(char) == "number" and (char < 0x20 or (char >= 0x7F and char <= 0x9F))
end

function keyboard.isControlDown()
  return (keyboard.pressedCodes[keyboard.keys.lcontrol] or keyboard.pressedCodes[keyboard.keys.rcontrol]) ~= nil
end

function keyboard.isKeyDown(charOrCode)
  checkArg(1, charOrCode, "string", "number")
  if type(charOrCode) == "string" then
    return keyboard.pressedChars[charOrCode]
  elseif type(charOrCode) == "number" then
    return keyboard.pressedCodes[charOrCode]
  end
end

function keyboard.isShiftDown()
  return (keyboard.pressedCodes[keyboard.keys.lshift] or keyboard.pressedCodes[keyboard.keys.rshift]) ~= nil
end

-------------------------------------------------------------------------------

kernel.userspace.package.preload.keyboard = keyboard

function start()
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
    
    kernel.modules.keventd.listen("key_down", onKeyDown)
    kernel.modules.keventd.listen("key_up", onKeyUp)
    kernel.modules.keventd.listen("component_unavailable", onComponentUnavailable)
end
