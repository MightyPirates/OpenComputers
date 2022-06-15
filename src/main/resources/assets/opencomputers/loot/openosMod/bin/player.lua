local guix = require("guix")
local midi = require("midi2")
local shell = require("shell")
local fs = require("filesystem")
local su = require("superUtiles")
local component = require("component")
local note = require("note")
local computer = require("computer")

local args, options = shell.parse(...)
if #args == 0 then
    print("Usage: player midfilepath")
    return
end
gui = guix.create()
gui.soundOn = false
local path = shell.resolve(args[1], "mid")

if not path or not fs.exists(path) then io.stderr:write("file not found") return end
if fs.isDirectory(path) then io.stderr:write("is directory") return end

local modes = {"all devices", "combined types", "one device"}
local modnum = 1
local volume = 1
local noteInstrument = 0

local soundDevices = {}
local beeps = {}
table.insert(soundDevices, function(n, d)
    table.insert(beeps, {n, d})
end)
function soundDevices.flush()
    local devicesCount = 0
    for address in component.list("note_block") do devicesCount = devicesCount + 1 end
    for address in component.list("iron_noteblock") do devicesCount = devicesCount + 1 end
    for address in component.list("beep") do devicesCount = devicesCount + 1 end

    if devicesCount > 0 then
        if modnum == 1 then
            for i = 1, #beeps do
                local beep = beeps[i]
                for address in component.list("note_block") do
                    component.invoke(address, "trigger", (note.midi(beep[1]) + 6 - 60) % 24 + 1)
                end
                for address in component.list("iron_noteblock") do
                    component.invoke(address, "playNote", noteInstrument, (note.midi(beep[1]) + 6 - 60) % 24, volume)
                end
                for address in component.list("beep") do
                    component.invoke(address, "beep", {[beep[1]] = beep[2]})
                end
            end
            beeps = {}
        elseif modnum == 2 then
            do
                local beeps = su.simpleTableClone(beeps)
                local interator = component.list("note_block")
                while #beeps > 0 do
                    local address = interator()
                    if not address then break end
                    component.invoke(address, "trigger", (note.midi(beeps[1][1]) + 6 - 60) % 24 + 1)
                    table.remove(beeps, 1)
                end
            end
            do
                local beeps = su.simpleTableClone(beeps)
                local interator = component.list("iron_noteblock")
                while #beeps > 0 do
                    local address = interator()
                    if not address then break end
                    component.invoke(address, "playNote", noteInstrument, (note.midi(beeps[1][1]) + 6 - 60) % 24, volume)
                    table.remove(beeps, 1)
                end
            end
            do
                local beeps = su.simpleTableClone(beeps)
                local interator = component.list("beep")
                while #beeps > 0 do
                    local address = interator()
                    if not address then break end
                    component.invoke(address, "beep", {[beeps[1][1]] = beeps[1][2]})
                    table.remove(beeps, 1)
                end
            end
            beeps = {}
        elseif modnum == 3 then
            local beepsfuncs = {}
            for address in component.list("note_block") do
                table.insert(beepsfuncs, function(n, d)
                    component.invoke(address, "trigger", (note.midi(n) + 6 - 60) % 24 + 1)
                end)
            end
            for address in component.list("iron_noteblock") do
                table.insert(beepsfuncs, function(n, d)
                    component.invoke(address, "playNote", noteInstrument, (note.midi(n) + 6 - 60) % 24, volume)
                end)
            end
            for address in component.list("beep") do
                table.insert(beepsfuncs, function(n, d)
                    component.invoke(address, "beep", {[n] = d})
                end)
            end
            while #beeps > 0 do
                beepsfuncs[1](beeps[1][1], beeps[1][2])
                table.remove(beepsfuncs, 1)
                table.remove(beeps, 1)
            end
        end
    else
        for i = 1, #beeps do
            computer.beep(beeps[i][1], beeps[i][2])
        end
        beeps = {}
    end
end

local mid = midi.create(path, soundDevices)
mid.min = 20
mid.max = 2000

local th

local function play()
    if th then th:resume() return end
    th = mid.createThread(true)
end

local function stop()
    if not th then return end
    th:kill()
    th = nil
end

local function pause()
    if not th then return end
    th:suspend()
end

-----------------------------------------

if gui.gpu.getDepth() > 1 then
    gui.gpu.setPaletteColor(0, 0x000000)
    gui.gpu.setPaletteColor(1, 0xFFFFFF)

    gui.gpu.setPaletteColor(2, 0x003300)
    gui.gpu.setPaletteColor(3, 0x005500)
    gui.gpu.setPaletteColor(4, 0x00FF00)

    gui.gpu.setPaletteColor(5, 0x330000)
    gui.gpu.setPaletteColor(6, 0x550000)
    gui.gpu.setPaletteColor(7, 0xFF0000)
end

rx, ry = math.max(gui.userX, 50), math.max(gui.userY, 16)
local resolutionOk = pcall(gui.gpu.setResolution, rx, ry)
if not resolutionOk then rx, ry = gui.gpu.maxResolution() end

scene = gui.createScene(gui.selectColor(0x003300, nil, false), rx, ry)
sceneCenterX, sceneCenterY = scene.getCenter()

startbutton = scene.createButton(sceneCenterX - 16, 1, 32, 1, "play", play)
startbutton.backColor = gui.selectColor(0x005500, nil, true)
startbutton.foreColor = gui.selectColor(0x00FF00, nil, false)
startbutton.invertBackColor = gui.selectColor(0x550000, nil, false)
startbutton.invertForeColor = gui.selectColor(0xFF0000, nil, true)

stopbutton = scene.createButton(sceneCenterX - 16, 2, 32, 1, "stop", stop)
stopbutton.backColor = gui.selectColor(0x005500, nil, true)
stopbutton.foreColor = gui.selectColor(0x00FF00, nil, false)
stopbutton.invertBackColor = gui.selectColor(0x550000, nil, false)
stopbutton.invertForeColor = gui.selectColor(0xFF0000, nil, true)

pausebutton = scene.createButton(sceneCenterX - 16, 3, 32, 1, "pause", pause)
pausebutton.backColor = gui.selectColor(0x005500, nil, true)
pausebutton.foreColor = gui.selectColor(0x00FF00, nil, false)
pausebutton.invertBackColor = gui.selectColor(0x550000, nil, false)
pausebutton.invertForeColor = gui.selectColor(0xFF0000, nil, true)

devicemodelabel = scene.createLabel(sceneCenterX - 16, 5, 32, 1, "sound mode: " .. modes[modnum])
devicemodelabel.backColor = gui.selectColor(0x005500, nil, true)
devicemodelabel.foreColor = gui.selectColor(0x00FF00, nil, false)

devicemodbutton = scene.createButton(sceneCenterX - 16, 6, 32, 1, "change mode", function()
    modnum = ((modnum) % #modes) + 1
    devicemodelabel.text = "sound mode: " .. modes[modnum]
    devicemodelabel.draw()
end)
devicemodbutton.backColor = gui.selectColor(0x005500, nil, true)
devicemodbutton.foreColor = gui.selectColor(0x00FF00, nil, false)
devicemodbutton.invertBackColor = gui.selectColor(0x550000, nil, false)
devicemodbutton.invertForeColor = gui.selectColor(0xFF0000, nil, true)

-----------------------------------------

local addSeekToSeek = 0
if ry <= 16 then
    addSeekToSeek = 4
end

speedseek = scene.createSeekbar(1, (sceneCenterY - 1) + addSeekToSeek, scene.sizeX, "speed    ", function(value) mid.speed = value end, 0, 0.2, 2, 1)
notespeedseek = scene.createSeekbar(1, (sceneCenterY) + addSeekToSeek, scene.sizeX, "notespeed", function(value) mid.noteduraction = value end, 0, 0.2, 2, 1)
pitchseek = scene.createSeekbar(1, (sceneCenterY + 1) + addSeekToSeek, scene.sizeX, "pitch    ", function(value) mid.pitch = value end, 0, 0.2, 2, 1)
noteinsseek = scene.createSeekbar(1, (sceneCenterY + 2) + addSeekToSeek, scene.sizeX, "note inst", function(value) noteInstrument = value end, 0, 0, 6, 0)
volumeseek = scene.createSeekbar(1, (sceneCenterY + 3) + addSeekToSeek, scene.sizeX, "volume   ", function(value) volume = value end, 0, 0.1, 1, 1)

speedseek.backColor = gui.selectColor(0x005500, nil, true)
speedseek.foreColor = gui.selectColor(0x00FF00, nil, false)

notespeedseek.backColor = gui.selectColor(0x005500, nil, true)
notespeedseek.foreColor = gui.selectColor(0x00FF00, nil, false)

pitchseek.backColor = gui.selectColor(0x005500, nil, true)
pitchseek.foreColor = gui.selectColor(0x00FF00, nil, false)

pitchseek.backColor = gui.selectColor(0x005500, nil, true)
pitchseek.foreColor = gui.selectColor(0x00FF00, nil, false)

volumeseek.backColor = gui.selectColor(0x005500, nil, true)
volumeseek.foreColor = gui.selectColor(0x00FF00, nil, false)

noteinsseek.backColor = gui.selectColor(0x005500, nil, true)
noteinsseek.foreColor = gui.selectColor(0x00FF00, nil, false)
noteinsseek.onlyIntegers = true

-----------------------------------------

gui.select(scene)
gui.run()