local gui = require("simpleGui3").create()
local term = require("term")
local computer = require("computer")
local fs = require("filesystem")

-----------------------------------------

local num
while true do
    num = gui.menu("Shortcut Menu", {"Shutdown", "Reboot", "Settings", "Event.log", "Dmesg", "Lua", "Back"}, num)
    gui.gpu.setBackground(0)
    gui.gpu.setForeground(0xFFFFFF)
    term.clear()
    if num == 1 then
        computer.shutdown()
    elseif num == 2 then
        computer.shutdown(true)
    elseif num == 3 then
        os.execute("settings")
    elseif num == 4 then
        if fs.exists("/tmp/event.log") then
            os.execute("edit /tmp/event.log")
        else
            gui.status("event.log is not found", true)
        end
    elseif num == 5 then
        os.execute("dmesg")
    elseif num == 6 then
        os.execute("lua")
    elseif num == 7 then
        gui.exit()
    end
end