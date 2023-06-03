local version = "OpenLoader 0.2EE"

local eeprom = [[
_G._OSVERSION = "]] .. version .. [["
local component = component or require('component')
local computer = computer or require('computer')
local unicode = unicode or require('unicode')

local eeprom = component.list("eeprom")()
computer.getBootAddress = function()
  return component.invoke(eeprom, "getData")
end
computer.setBootAddress = function(address)
  return component.invoke(eeprom, "setData", address)
end

local gpu = component.list("gpu")()
local w, h

local screen = component.list('screen')()
for address in component.list('screen') do
    if #component.invoke(address, 'getKeyboards') > 0 then
        screen = address
    end
end

local cls = function()end
if gpu and screen then
    component.invoke(gpu, "bind", screen)
    w, h = component.invoke(gpu, "getResolution")
    component.invoke(gpu, "setResolution", w, h)
    component.invoke(gpu, "setBackground", 0x000000)
    component.invoke(gpu, "setForeground", 0xFFFFFF)
    component.invoke(gpu, "fill", 1, 1, w, h, " ")
    cls = function()component.invoke(gpu,"fill", 1, 1, w, h, " ")end
end
local y = 1
local function status(msg)
    if gpu and screen then
        component.invoke(gpu, "set", 1, y, msg)
        if y == h then
            component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
            component.invoke(gpu, "fill", 1, h, w, 1, " ")
        else
            y = y + 1
        end
    end
end

local function loadfile(fs, file)
    --status("> " .. file)
    local handle, reason = component.invoke(fs,"open",file)
    if not handle then
        error(reason)
    end
    local buffer = ""
    repeat
        local data, reason = component.invoke(fs,"read",handle,math.maxinteger or math.huge)
        if not data and reason then
            error(reason)
        end
        buffer = buffer .. (data or "")
    until not data
    component.invoke(fs,"close",handle)
    return load(buffer, "=" .. file)
end

local function dofile(fs, file)
    local program, reason = loadfile(fs, file)
    if program then
        local result = table.pack(pcall(program))
        if result[1] then
            return table.unpack(result, 2, result.n)
        else
            error(result[2])
        end
    else
        error(reason)
    end
end

local function boot(kernel)
    status("BOOTING")
    _G.computer.getBootAddress = function()return kernel.address end
    cls()
    dofile(kernel.address, kernel.fpx .. kernel.file)
end

status(_OSVERSION)
status("Select what to boot:")

local osList = {}

for fs in component.list("filesystem") do
    if component.invoke(fs, "isDirectory", "boot/kernel/")then
        for _,file in ipairs(component.invoke(fs, "list", "boot/kernel/")) do
            osList[#osList+1] = {fpx = "boot/kernel/", file = file, address = fs}
            status(tostring(#osList).."."..file.." from "..(fs:sub(1,3)))
        end
    end
    if component.invoke(fs, "exists", "init.lua") then
        local osName = "init.lua"
        if component.invoke(fs, "exists", ".osprop") then
            pcall(function()
                local prop = dofile(fs, ".osprop") 
                osName = (prop and prop.name) or "init.lua" 
            end)
        end
        osList[#osList+1] = {fpx = "", file = "init.lua", address = fs}
        status(tostring(#osList).."."..osName.." from "..(fs:sub(1,3)))
    end
end
status("Select os: ")
if #osList == 1 then
    boot(osList[1])
end
if #osList == 0 then
    error("No OS found")
    while true do computer.pullSignal() end
end
while true do
    local sig = {computer.pullSignal()}
    if sig[1] == "key_down" then
        if sig[4] >= 2 and sig[4] <= 11 then
            if osList[sig[4]-1] then
                boot(osList[sig[4]-1])
            else
                status("Not found!")
            end
        end
    end
end
error("System crashed")
while true do computer.pullSignal() end
]]

local component = require "component"
local computer = require "computer"
local shell = require "shell"
local term = require "term"

local args, options = shell.parse(...)

if options.h or options.help then
  print("opl-flash [-hqr] [--label=EEPROMLabel]")
  print("        --label=  Set specified label for the EEPROM")
  print("    -q  --quiet   Do not output anything, and don't ask if sure")
  print("    -r  --reboot  Reboot after installing")
  print("    -h  --help    Display this help")
  return
end

local say = not (options.q or options.quiet) and print or function()end
say ("Do you really want to flash openloader to EEPROM("..tostring(#eeprom).." bytes)[Y/n]")

if options.q or options.quiet or ((io.read() or "n").."y"):match("^%s*[Yy]") then
  say("Flashing... Do not reboot now!")
  component.eeprom.set(eeprom)
  component.eeprom.setLabel((type(options.label) == "string" and options.label) or version)
  if options.r or options.reboot then
    computer.shutdown(true)
  else
    say("Done. you can reboot now")
  end
end

