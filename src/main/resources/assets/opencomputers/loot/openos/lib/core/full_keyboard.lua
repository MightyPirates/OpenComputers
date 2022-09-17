local keyboard = require("keyboard")

-- Keyboard codes used by GLFW
keyboard.keys["1"]           = 0x31
keyboard.keys["2"]           = 0x32
keyboard.keys["3"]           = 0x33
keyboard.keys["4"]           = 0x34
keyboard.keys["5"]           = 0x35
keyboard.keys["6"]           = 0x36
keyboard.keys["7"]           = 0x37
keyboard.keys["8"]           = 0x38
keyboard.keys["9"]           = 0x39
keyboard.keys["0"]           = 0x30
keyboard.keys.a               = 0x41
keyboard.keys.b               = 0x42
keyboard.keys.c               = 0x43
keyboard.keys.d               = 0x44
keyboard.keys.e               = 0x45
keyboard.keys.f               = 0x46
keyboard.keys.g               = 0x47
keyboard.keys.h               = 0x48
keyboard.keys.i               = 0x49
keyboard.keys.j               = 0x4A
keyboard.keys.k               = 0x4B
keyboard.keys.l               = 0x4C
keyboard.keys.m               = 0x4D
keyboard.keys.n               = 0x4E
keyboard.keys.o               = 0x4F
keyboard.keys.p               = 0x50
keyboard.keys.q               = 0x51
keyboard.keys.r               = 0x52
keyboard.keys.s               = 0x53
keyboard.keys.t               = 0x54
keyboard.keys.u               = 0x55
keyboard.keys.v               = 0x56
keyboard.keys.w               = 0x57
keyboard.keys.x               = 0x58
keyboard.keys.y               = 0x59
keyboard.keys.z               = 0x5A

keyboard.keys.apostrophe      = 0x27
keyboard.keys.at              = -1 -- removed (GLFW)
keyboard.keys.back            = 0x103 -- backspace
keyboard.keys.backslash       = 0x5C
keyboard.keys.capital         = 0x118 -- capslock
keyboard.keys.colon           = -1 -- removed (GLFW)
keyboard.keys.comma           = 0x2C
keyboard.keys.enter           = 0x101
keyboard.keys.equals          = 0x3D
keyboard.keys.grave           = 0x60 -- accent grave
keyboard.keys.lbracket        = 0x5B
keyboard.keys.lcontrol        = 0x155
keyboard.keys.lmenu           = 0x156 -- left Alt
keyboard.keys.lshift          = 0x154
keyboard.keys.minus           = -1 -- removed (GLFW)
keyboard.keys.numlock         = 0x11A
keyboard.keys.pause           = 0x11C
keyboard.keys.period          = 0x2E
keyboard.keys.rbracket        = 0x5D
keyboard.keys.rcontrol        = 0x159
keyboard.keys.rmenu           = 0x15A -- right Alt
keyboard.keys.rshift          = 0x158
keyboard.keys.scroll          = 0x119 -- Scroll Lock
keyboard.keys.semicolon       = 0x3B
keyboard.keys.slash           = 0x2F -- / on main keyboard
keyboard.keys.space           = 0x20
keyboard.keys.stop            = -1 -- removed (GLFW)
keyboard.keys.tab             = 0x102
keyboard.keys.underline       = -1 -- removed (GLFW)

-- Keypad (and numpad with numlock off)
keyboard.keys.up                   = 0x109
keyboard.keys.down                 = 0x108
keyboard.keys.left                 = 0x107
keyboard.keys.right                = 0x106
keyboard.keys.home                 = 0x10C
keyboard.keys["end"]               = 0x10D
keyboard.keys.pageUp               = 0x10A
keyboard.keys.pageDown             = 0x10B
keyboard.keys.insert               = 0x104
keyboard.keys.delete               = 0x105

-- Function keys
keyboard.keys.f1              = 0x122
keyboard.keys.f2              = 0x123
keyboard.keys.f3              = 0x124
keyboard.keys.f4              = 0x125
keyboard.keys.f5              = 0x126
keyboard.keys.f6              = 0x127
keyboard.keys.f7              = 0x128
keyboard.keys.f8              = 0x129
keyboard.keys.f9              = 0x12A
keyboard.keys.f10             = 0x12B
keyboard.keys.f11             = 0x12C
keyboard.keys.f12             = 0x12D
keyboard.keys.f13             = 0x12E
keyboard.keys.f14             = 0x12F
keyboard.keys.f15             = 0x130
keyboard.keys.f16             = 0x131
keyboard.keys.f17             = 0x132
keyboard.keys.f18             = 0x133
keyboard.keys.f19             = 0x134
keyboard.keys.f20             = 0x135
keyboard.keys.f21             = 0x136
keyboard.keys.f22             = 0x137
keyboard.keys.f23             = 0x138
keyboard.keys.f24             = 0x139
keyboard.keys.f25             = 0x13A

-- Japanese keyboards (removed in GLFW)
keyboard.keys.kana            = -1
keyboard.keys.kanji           = -1
keyboard.keys.convert         = -1
keyboard.keys.noconvert       = -1
keyboard.keys.yen             = -1
keyboard.keys.circumflex      = -1
keyboard.keys.ax              = -1

-- Numpad
keyboard.keys.numpad0         = 0x140
keyboard.keys.numpad1         = 0x141
keyboard.keys.numpad2         = 0x142
keyboard.keys.numpad3         = 0x143
keyboard.keys.numpad4         = 0x144
keyboard.keys.numpad5         = 0x145
keyboard.keys.numpad6         = 0x146
keyboard.keys.numpad7         = 0x147
keyboard.keys.numpad8         = 0x148
keyboard.keys.numpad9         = 0x149
keyboard.keys.numpadmul       = 0x14C
keyboard.keys.numpaddiv       = 0x14B
keyboard.keys.numpadsub       = 0x14D
keyboard.keys.numpadadd       = 0x14E
keyboard.keys.numpaddecimal   = 0x14A
keyboard.keys.numpadcomma     = -1 -- removed (GLFW)
keyboard.keys.numpadenter     = 0x14F
keyboard.keys.numpadequals    = 0x150

-- Create inverse mapping for name lookup.
setmetatable(keyboard.keys,
{
  __index = function(tbl, k)
    if type(k) ~= "number" then return end
    for name,value in pairs(tbl) do
      if value == k then
        return name
      end
    end
  end
})
