local gui = require("simpleGui2").create()
local su = require("superUtiles")
local fs = require("filesystem")
local term = require("term")
local component = require("component")

-----------------------------------------

local softs = {}
local strs = {}
for address in component.list("filesystem") do
    local proxy = component.proxy(address)
    local path
    for lfs, lpath in fs.mounts() do
        if lfs.address == proxy.address then
            path = lpath
            break
        end
    end
    if not path then goto skip end

    local full_path = fs.concat(path, ".soft.lua")
    if fs.exists(full_path) then
        table.insert(softs, full_path)
        table.insert(strs, "address: " .. proxy.address:sub(1, 4))

        local full_path = fs.concat(path, ".softinfo.txt")
        if fs.exists(full_path) then
            strs[#strs] = strs[#strs] .. ", info: " .. su.getFile(full_path)
        end
    end
    ::skip::
end
table.insert(strs, "back")

os.sleep(1)

-----------------------------------------

local num
while true do
    num = gui.menu("soft in drives", strs, num)
    if num > #softs then gui.exit() end
    local path = softs[num]
    
    gui.gpu.setBackground(0)
    gui.gpu.setForeground(0xFFFFFF)
    term.clear()
    os.execute(path)
    os.sleep(0.1)
end
