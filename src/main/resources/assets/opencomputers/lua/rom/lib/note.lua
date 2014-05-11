--Provides all music notes in range of computer.beep in MIDI and frequency form
--Author: Vexatos
local computer = require("computer")

local note = {}
--The table that maps note names to their respective MIDI codes
local notes = {}
--The reversed table "notes"
local reverseNotes = {}

do
  --All the base notes
  local tempNotes = {
    "c",
    "c#",
    "d",
    "d#",
    "e",
    "f",
    "f#",
    "g",
    "g#",
    "a",
    "a#",
    "b"
    }
  --The table containing all the standard notes and # semitones in correct order, temporarily
  local sNotes = {}
  --The table containing all the b semitones
  local bNotes = {}

  --Registers all possible notes in order
  do
    table.insert(sNotes,"a0")
    table.insert(sNotes,"a#0")
    table.insert(bNotes,"bb0")
    table.insert(sNotes,"b0")
    for i = 1,6 do
      for _,v in ipairs(tempNotes) do
        table.insert(sNotes,v..tostring(i))
        if #v == 1 and v ~= "c" and v ~= "f" then
          table.insert(bNotes,v.."b"..tostring(i))
        end
      end
    end
  end
  for i=21,95 do
    notes[sNotes[i-20]]=tostring(i)
  end

  --Reversing the whole table in reverseNotes, used for note.get
  do
    for k,v in pairs(notes) do
      reverseNotes[tonumber(v)]=k
    end
  end

  --This is registered after reverseNotes to avoid conflicts
  for k,v in ipairs(bNotes) do
    notes[v]=tostring(notes[string.gsub(v,"(.)b(.)","%1%2")]-1)
  end
end

--Converts string or frequency into MIDI code
function note.midi(n)
  if type(n) == "string" then
    n = string.lower(n)
    if tonumber(notes[n])~=nil then
      return tonumber(notes[n])
    else
      error("Wrong input "..tostring(n).." given to note.midi, needs to be <note>[semitone sign]<octave>, e.g. A#0 or Gb4")
    end
  elseif type(n) == "number" then
    return math.floor((12*math.log(n/440,2))+69)
  else
    error("Wrong input "..tostring(n).." given to note.midi, needs to be a number or a string")
  end
end

--Converts String or MIDI code into frequency
function note.freq(n)
  if type(n) == "string" then
    n = string.lower(n)
    if tonumber(notes[n])~=nil then
      return math.pow(2,(tonumber(notes[n])-69)/12)*440
    else
      error("Wrong input "..tostring(n).." given to note.freq, needs to be <note>[semitone sign]<octave>, e.g. A#0 or Gb4",2)
    end
  elseif type(n) == "number" then
    return math.pow(2,(n-69)/12)*440
  else
    error("Wrong input "..tostring(n).." given to note.freq, needs to be a number or a string",2)
  end
end

--Converts a MIDI value back into a string
function note.name(n)
  n = tonumber(n)
  if reverseNotes[n] then
    return string.upper(string.match(reverseNotes[n],"^(.)"))..string.gsub(reverseNotes[n],"^.(.*)","%1")
  else
    error("Attempt to get a note for a non-exsisting MIDI code",2)
  end
end

--Converts Note block ticks (0-24) to MIDI code (34-58) and vice-versa
function note.ticks(n)
  if type(n) == "number" then
    if n>=0 and n<=24 then
      return n+34
    elseif n>=34 and n<=58 then
      return n-34
    else
      error("Wrong input "..tostring(n).." given to note.ticks, needs to be a number [0-24 or 34-58]",2)
    end
  else
    error("Wrong input "..tostring(n).." given to note.ticks, needs to be a number",2)
  end
end

--Plays a tone, input is either the note as a string or the MIDI code as well as the duration of the tone
function note.play(tone,duration)
  computer.beep(note.freq(tone),duration)
end

return note
