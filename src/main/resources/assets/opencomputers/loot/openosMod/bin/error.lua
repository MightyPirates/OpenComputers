local shell = require("shell")
local computer = require("computer")
local term = require("term")
local unicode = require("unicode")
local computer = require("computer")
local su = require("superUtiles")
local event = require("event")
local process = require("process")

local gpu = term.gpu()

--------------------------------------------

local args, options = shell.parse(...)

local mainMessage = "Fatal error" .. ((args[1] and (" (" .. args[1] .. ")")) or "")
if not term.isAvailable() then
    computer.pullSignal = function()
        error(mainMessage, 0)
    end
    computer.pullSignal()
end

process.info().data.signal = function()
end

--------------------------------------------

local w, h = gpu.getResolution()
local wC, hC = w / 2, h / 2 

local triangle = {
    "     ◢█◣",
    "    ◢███◣",
    "   ◢█████◣",
    "  ◢███████◣",
    " ◢█████████◣",
    "◢███████████◣"
}

local warn = {
    "█",
    "█",
    "█",
    "",
    "▀",
}

local trianglePosX = math.ceil(wC - 6)
local trianglePosY = math.ceil(hC - 7)

gpu.setBackground(0x000000)
gpu.setForeground(0xff0000)
gpu.fill(1, 1, w, h, " ")

for str = 1, #triangle do 
    gpu.set(trianglePosX, str + trianglePosY, triangle[str])
end

gpu.setBackground(0xff0000)
if math.floor(gpu.getDepth()) == 1 then
    gpu.setForeground(0)
else
    gpu.setForeground(0xffffff)
end

for str = 1, #warn do 
    gpu.set(trianglePosX + 6, trianglePosY + str + 1, warn[str])
end

gpu.setBackground(0x000000)
gpu.setForeground(0xffffff)

local powerMessage = "Press power button to shutdown"
if su.isTouchScreen(term.screen()) and term.keyboard() then
    powerMessage = "Press power button, touch to screen or press enter to shutdown"
elseif su.isTouchScreen(term.screen()) then
    powerMessage = "Press power button or touch to screen to shutdown"
elseif term.keyboard() then
    powerMessage = "Press power button or press enter to shutdown"
end

local strs = {mainMessage, powerMessage}
for i, v in ipairs(strs) do
    gpu.set(math.floor(wC - (unicode.len(v) // 2)), hC + i + 2, v)
end

while true do
    local eventData = {event.pull()}
    if eventData[1] == "key_down" and su.inTable(term.keyboards(), eventData[2]) and eventData[4] == 28 then
        break
    elseif eventData[1] == "touch" and eventData[2] == term.screen() then
        break
    end
end

computer.shutdown()