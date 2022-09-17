local component = require("component")

local args = {...}
local screen = args[1]

local keyboards = {}
for _, kbd in pairs(component.invoke(screen, "getKeyboards")) do
    keyboards[kbd] = true
end

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
  a               = 0x41,
  b               = 0x42,
  c               = 0x43,
  d               = 0x44,
  e               = 0x45,
  f               = 0x46,
  g               = 0x47,
  h               = 0x48,
  i               = 0x49,
  j               = 0x4A,
  k               = 0x4B,
  l               = 0x4C,
  m               = 0x4D,
  n               = 0x4E,
  o               = 0x4F,
  p               = 0x50,
  q               = 0x51,
  r               = 0x52,
  s               = 0x53,
  t               = 0x54,
  u               = 0x55,
  v               = 0x56,
  w               = 0x57,
  x               = 0x58,
  y               = 0x59,
  z               = 0x5A,

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

do
  local keys = {}
  for k in pairs(keyboard.keys) do
    table.insert(keys, k)
  end
  for _, k in pairs(keys) do
    keyboard.keys[keyboard.keys[k]] = k
  end
end

function keyboard.isControl(char)
    return type(char) == "number" and (char < 0x20 or (char >= 0x7F and char <= 0x9F))
end

local on = {}

function on.key_down(_, source, ascii, keycode, user)
    if not keyboards[source] then return end
    keyboard.pressedChars[ascii] = true
    keyboard.pressedCodes[keycode] = true
    
    if ascii == 13 then ascii = 10 end
    if ascii ~= 0 and ascii ~= 127 then 
        io.stdout:write(unicode.char(ascii))
    else
        if keycode == keyboard.up then io.stdout:write("\x1b[A")
        elseif keycode == keyboard.down then io.stdout:write("\x1b[B")
        elseif keycode == keyboard.right then io.stdout:write("\x1b[C")
        elseif keycode == keyboard.left then io.stdout:write("\x1b[D")
            
        elseif keycode == keyboard.keys.f1 then io.stdout:write("\x1bOP")
        elseif keycode == keyboard.keys.f2 then io.stdout:write("\x1bOQ")
        elseif keycode == keyboard.keys.f3 then io.stdout:write("\x1bOR")
        elseif keycode == keyboard.keys.f4 then io.stdout:write("\x1bOS")
        
        elseif keycode == keyboard.keys.delete then io.stdout:write("\x1b[3~")
        --elseif keycode == keyboard.keys.insert then io.stdout:write("\x1b[2~")
        elseif keycode == keyboard.keys.pageUp then io.stdout:write("\x1b[5~")
        elseif keycode == keyboard.keys.pageDown then io.stdout:write("\x1b[6~")
        elseif keycode == keyboard.keys.home then io.stdout:write("\x1bOH")
        elseif keycode == keyboard.keys["end"] then io.stdout:write("\x1bOF")
        elseif keycode == keyboard.keys.tab then io.stdout:write("\t")
        --TODO: rest fX keys
        end
    end
end

function on.key_up(_, source, ascii, keycode, user)
    if not keyboards[source] then return end
    keyboard.pressedChars[ascii] = nil
    keyboard.pressedCodes[keycode] = nil
end

function on.clipboard(_, source, data, user)
    if not keyboards[source] then return end
    io.stdout:write(data)
end

while true do
    local signal = {computer.pullSignal()}
    if on[signal[1]] then
       on[signal[1]](table.unpack(signal))
    end
end
