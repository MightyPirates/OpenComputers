local shell = require("shell")
local component = require("component")
local keyboard = require("keyboard")
local event = require("event")

local args,options = shell.parse(...)

local MagicTable = {}

--stuff to make life easier
local note
local gpu

--this saves me from typing "keyboard" over and over again
local keys = keyboard.keys

local function MakeTheMagicHappen() --Fill MagicTable with keybindings
  MagicTable[keys["1"]] = 1
  MagicTable[keys["q"]] = 2
  MagicTable[keys["2"]] = 3
  MagicTable[keys["w"]] = 4
  MagicTable[keys["3"]] = 5
  MagicTable[keys["e"]] = 6
  MagicTable[keys["r"]] = 7
  MagicTable[keys["5"]] = 8
  MagicTable[keys["t"]] = 9
  MagicTable[keys["6"]] = 10
  if options.z then
    MagicTable[keys["z"]] = 11
  else
    MagicTable[keys["y"]] = 11
  end
  MagicTable[keys["u"]] = 12
  MagicTable[keys["8"]] = 13
  MagicTable[keys["i"]] = 14
  MagicTable[keys["9"]] = 15
  MagicTable[keys["o"]] = 16
  MagicTable[keys["0"]] = 17
  MagicTable[keys["p"]] = 18
  if options.z then
    MagicTable[keys["y"]] = 19
  else
    MagicTable[keys["z"]] = 19
  end
  MagicTable[keys["s"]] = 20
  MagicTable[keys["x"]] = 21
  MagicTable[keys["d"]] = 22
  MagicTable[keys["c"]] = 23
  MagicTable[keys["v"]] = 24
  MagicTable[keys["g"]] = 25
end

if component.isAvailable("note_block") then
  --A note block is attached so go on with the stuff

  --some more stuff to make life easier
gpu = component.gpu
note = component.note_block

else
  error("No compatible Noteblock detected, please check your OpenComponenets and your cables")
end

if options.y then
  print("QWERTY-MODE activated")
elseif options.z then
  print("QWERTZ-MODE activated")
else
  print("OC-Piano 1.0 by Wuerfel_21\nPlease set your Keyboard-Layout:\npiano -y for QWERTY\npiano -z for QWERTZ")
  return "why are you reading this?"
end

print("Keys from lowest pitch to highest pitch: 1, Q, 2, W, 3, E, R, 5, T, 6, Y(Z on QWERTZ-MODE!), U, 8, I, 9, O, 0, P, Z(Y on QWERTZ-MODE!), S, X, D, C, V, G \nIf this is too complicated: Minecraft Note Block studio has the same mapping(so try it out)")

MakeTheMagicHappen()
while 1 do
  local stuff,address,char,code,user = event.pull("key_down")
  if code == keys["c"] and keyboard.isControlDown() then
    return "good job"
  end
  if MagicTable[code] then
    if MagicTable[code] <= 25 and MagicTable[code] >=1 then
      note.trigger(MagicTable[code])
    end
  end
end