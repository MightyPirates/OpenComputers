local thread = require("thread")

-------------------------------------------------------

local count = 0
local function interrupt()
  count = count + 1
  if count % 1024 == 0 then
    os.sleep()
  end
end

-------------------------------------------------------

local lib = {}

function lib.create(filepath, instruments, notemode, pitch, speed, noteduraction)
    local obj = {}
    obj.filepath = filepath
    obj.speed = speed or 1
    obj.noteduraction = noteduraction or speed or 1
    obj.pitch = pitch or 1
    obj.instruments = instruments
    obj.notemode = notemode or false
    obj.min = false
    obj.max = false

    function obj.play()
        local component = require("component")
        local computer = require("computer")
        local shell = require("shell")
        local keyboard = require("keyboard")
        local note = require("note")
        local bit32 = require("bit32")

        local instruments = obj.instruments
        local filename = obj.filepath
    
        local function beepableFrequency(midiCode)
            local freq
            if obj.notemode then
                freq = midiCode
            else
                freq = note.freq(midiCode)
            end
            freq = freq * obj.pitch
            if obj.min and obj.max then
                while freq < obj.min do freq = freq * 2 end
                while freq > obj.max do freq = freq / 2 end
            end
            return freq
        end
    
        local f, reason = io.open(filename, "rb")
        if not f then
            return nil, reason
        end
    
        local function parseVarInt(s, bits) -- parses multiple bytes as an integer
            if not s then
                return nil, "error parsing file"
            end
            bits = bits or 8
            local mask = bit32.rshift(0xFF, 8 - bits)
            local num = 0
            for i = 1, s:len() do
                num = num + bit32.lshift(bit32.band(s:byte(i), mask), (s:len() - i) * bits)
            end
            return num
        end
    
        local function readChunkInfo() -- reads chunk header info
            local id = f:read(4)
            if not id then
                return
            end
            return id, parseVarInt(f:read(4))
        end
    
        -- Read the file header and with if file information.
        local id, size = readChunkInfo()
        if id ~= "MThd" or size ~= 6 then
            return nil, "error parsing header (" .. id .. "/" .. size .. ")"
        end
    
        local format = parseVarInt(f:read(2))
        local tracks = parseVarInt(f:read(2))
        local delta = parseVarInt(f:read(2))
    
        if format < 0 or format > 2 then
            return nil, "unknown format"
        end
    
        local formatName = ({"single", "synchronous", "asynchronous"})[format + 1]
        --print(string.format("Found %d %s tracks.", tracks, formatName))
    
        if format == 2 then
            return nil, "Sorry, asynchronous tracks are not supported."
        end
    
        -- Figure out our time system and prepare accordingly.
        local time = {division = bit32.band(0x8000, delta) == 0 and "tpb" or "fps"}
        if time.division == "tpb" then
            time.tpb = bit32.band(0x7FFF, delta)
            time.mspb = 500000
            function time.tick()
                return time.mspb / time.tpb
            end
            --print(string.format("Time division is in %d ticks per beat.", time.tpb))
        else
            time.fps = bit32.band(0x7F00, delta)
            time.tpf = bit32.band(0x00FF, delta)
            function time.tick()
                return 1000000 / (time.fps * time.tpf)
            end
            --print(string.format("Time division is in %d frames per second with %d ticks per frame.", time.fps, time.tpf))
        end
        function time.calcDelay(later, earlier)
            return (later - earlier) * time.tick() / 1000000
        end
    
        -- Parse all track chunks.
        local totalOffset = 0
        local totalLength = 0
        local tracks = {}
        while true do
            interrupt()            
            local id, size = readChunkInfo()
            if not id then
                break
            end
            if id == "MTrk" then
                local track = {}
                local cursor = 0
                local start, offset = f:seek(), 0
                local inSysEx = false
                local running = 0
    
                local function read(n)
                    n = n or 1
                    if n > 0 then
                        offset = offset + n
                        return f:read(n)
                    end
                end
                local function readVariableLength()
                    local total = ""
                    for i = 1, math.huge do
                        local part = read()
                        total = total .. part
                        if bit32.band(0x80, part:byte(1)) == 0 then
                            return parseVarInt(total, 7)
                        end
                    end
                end
                local function parseVoiceMessage(event)
                    local channel = bit32.band(0xF, event)
                    local note = parseVarInt(read())
                    local velocity = parseVarInt(read())
                    return channel, note, velocity
                end
                local currentNoteEvents = {}
                local function noteOn(cursor, channel, note, velocity)
                    track[cursor] = {channel, note, velocity}
                    if not currentNoteEvents[channel] then
                        currentNoteEvents[channel] = {}
                    end
                    currentNoteEvents[channel][note] = {event=track[cursor], tick=cursor}
                end
                local function noteOff(cursor, channel, note, velocity)
                    if not (currentNoteEvents[channel] and currentNoteEvents[channel][note]) then return end
                    table.insert(currentNoteEvents[channel][note].event
                            , time.calcDelay(cursor, currentNoteEvents[channel][note].tick))
                    currentNoteEvents[channel][note] = nil
                end
    
                while offset < size do
                    interrupt()                    
                    cursor = cursor + readVariableLength()
                    totalLength = math.max(totalLength, cursor)
                    local test = parseVarInt(read())
                    if inSysEx and test ~= 0xF7 then
                        return nil, "corrupt file: could not find continuation of divided sysex event"
                    end
                    local event
                    if bit32.band(test, 0x80) == 0 then
                        if running == 0 then
                            return nil, "corrupt file: invalid running status"
                        end
                        f.bufferRead = string.char(test) .. f.bufferRead
                        offset = offset - 1
                        event = running
                    else
                        event = test
                        if test < 0xF0 then
                            running = test
                        end
                    end
                    local status = bit32.band(0xF0, event)
                    if status == 0x80 then -- Note off.
                        local channel, note, velocity = parseVoiceMessage(event)
                        noteOff(cursor, channel, note, velocity)
                    elseif status == 0x90 then -- Note on.
                        local channel, note, velocity = parseVoiceMessage(event)
                        if velocity == 0 then
                            noteOff(cursor, channel, note, velocity)
                        else
                            noteOn(cursor, channel, note, velocity)
                        end
                    elseif status == 0xA0 then -- Aftertouch / key pressure
                        parseVoiceMessage(event) -- not handled
                    elseif status == 0xB0 then -- Controller
                        parseVoiceMessage(event) -- not handled
                    elseif status == 0xC0 then -- Program change
                        parseVarInt(read()) -- not handled
                    elseif status == 0xD0 then -- Channel pressure
                        parseVarInt(read()) -- not handled
                    elseif status == 0xE0 then -- Pitch / modulation wheel
                        parseVarInt(read(2), 7) -- not handled
                    elseif event == 0xF0 then -- System exclusive event
                        local length = readVariableLength()
                        if length > 0 then
                            read(length - 1)
                            inSysEx = read(1):byte(1) ~= 0xF7
                        end
                    elseif event == 0xF1 then -- MIDI time code quarter frame
                        parseVarInt(read()) -- not handled
                    elseif event == 0xF2 then -- Song position pointer 
                        parseVarInt(read(2), 7) -- not handled
                    elseif event == 0xF3 then -- Song select
                        parseVarInt(read(2), 7) -- not handled
                    elseif event == 0xF7 then -- Divided system exclusive event
                        local length = readVariableLength()
                        if length > 0 then
                            read(length - 1)
                            inSysEx = read(1):byte(1) ~= 0xF7
                        else
                            inSysEx = false
                        end
                    elseif event >= 0xF8 and event <= 0xFE then -- System real-time event
                        -- not handled
                    elseif event == 0xFF then
                        -- Meta message.
                        local metaType = parseVarInt(read())
                        local length = parseVarInt(read())
                        local data = read(length)
    
                        if metaType == 0x00 then -- Sequence number
                            track.sequence = parseVarInt(data)
                        elseif metaType == 0x01 then -- Text event
                        elseif metaType == 0x02 then -- Copyright notice
                        elseif metaType == 0x03 then -- Sequence / track name
                            track.name = data
                        elseif metaType == 0x04 then -- Instrument name
                            track.instrument = data
                        elseif metaType == 0x05 then -- Lyric text
                        elseif metaType == 0x06 then -- Marker text
                        elseif metaType == 0x07 then -- Cue point
                        elseif metaType == 0x20 then -- Channel prefix assignment
                        elseif metaType == 0x2F then -- End of track
                            track.eot = cursor
                        elseif metaType == 0x51 then -- Tempo setting
                            track[cursor] = parseVarInt(data)
                        elseif metaType == 0x54 then -- SMPTE offset
                        elseif metaType == 0x58 then -- Time signature
                        elseif metaType == 0x59 then -- Key signature
                        elseif metaType == 0x7F then -- Sequencer specific event
                        end
                    else
                        f:seek("cur", -9)
                        local area = f:read(16)
                        local dump = ""
                        for i = 1, area:len() do
                            dump = dump .. string.format(" %02X", area:byte(i))
                            if i % 4 == 0 then
                                dump = dump .. "\n"
                            end
                        end
                        return nil, string.format("midi file contains unhandled event types:\n0x%X at offset %d/%d\ndump of the surrounding area:\n%s", event, offset, size, dump)
                    end
                end
                -- turn off any remaining notes
                for iChannel, iNotes in pairs(currentNoteEvents) do
                    for iNote, iEntry in pairs(currentNoteEvents[iChannel]) do
                        noteOff(cursor, iChannel, iNote)
                    end
                end
                local delta = size - offset
                if delta ~= 0 then
                    f:seek("cur", delta)
                end
                totalOffset = totalOffset + size
                table.insert(tracks, track)
            else
                --print(string.format("Encountered unknown chunk type %s, skipping.", id))
                f:seek("cur", size)
            end
        end
    
        f:close()
    
        --print("Playing " .. #tracks .. " tracks:")
        for _, track in ipairs(tracks) do
            if track.name then
                --print(string.format("%s", track.name))
            end
        end
    
        local channels = {n=0}
        local lastTick, lastTime = 0, computer.uptime()
        --print("Press Ctrl+C to exit.")
        for tick = 1, totalLength do
            local hasEvent = false
            for _, track in ipairs(tracks) do
                if track[tick] then
                    hasEvent = true
                    break
                end
            end
            if hasEvent then
                local delay = time.calcDelay(tick, lastTick) / obj.speed
                -- delay % 0.05 == 0 doesn't seem to work
                if math.floor(delay * 100 + 0.5) % 5 == 0 or true then
                    os.sleep(delay)
                else
                    -- Busy idle otherwise, because a sleep will take up to 50ms.
                    local begin = os.clock()
                    while os.clock() - begin < delay do end
                end
                lastTick = tick
                lastTime = computer.uptime()
                for _, track in ipairs(tracks) do
                    local event = track[tick]
                    os.sleep()
                    if event then
                        if type(event) == "number" then
                            time.mspb = event
                        elseif type(event) == "table" then
                            local channel, note, velocity, duration = table.unpack(event)
                            local instrument
                            if not channels[channel] then
                                channels.n = channels.n + 1
                                channels[channel] = instruments[1 + ((channels.n - 1) % #instruments)]
                            end
                            if not duration then duration = 0 end
                            if not note then note = 1 end
                            if not channel then channel = "nil" end
                            if channels[channel] and channels[channel](beepableFrequency(note), duration / obj.noteduraction) then
                                break
                            end
                        end
                    end
                end
                if instruments.flush then instruments.flush() end
            end
        end
        return true
    end

    obj.loop = function()
        while true do
            local ok, err = obj.play()
            if not ok then
                return nil, err
            end
            os.sleep()
        end
    end

    obj.createThread = function(loop)
        if loop then
            return thread.create(function() local ok, err = obj.loop() if not ok then error(err) end end)
        else
            return thread.create(function() local ok, err = obj.play() if not ok then error(err) end end)
        end
    end

    return obj
end

return lib