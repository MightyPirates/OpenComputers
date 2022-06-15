local shell = require("shell")
local term = require("term")
local fs = require("filesystem")
local su = require("superUtiles")

if not term.isAvailable() then return end
local gpu = term.gpu()

local args, shell = shell.parse(...)

---------------------------------------------

if args[1] == "set" then
    if fs.exists("/etc/depth.cfg") then
        local depth = su.getFile("/etc/depth.cfg")
        if depth == "max" then
            gpu.setDepth(gpu.maxDepth())
        elseif tonumber(depth) then
            depth = tonumber(depth)
            if depth > math.floor(gpu.maxDepth(depth)) then
                depth = math.floor(gpu.maxDepth(depth))
                su.saveFile("/etc/depth.cfg", "max")
            end
            gpu.setDepth(depth)
        else
            gpu.setDepth(gpu.maxDepth())
            su.saveFile("/etc/depth.cfg", "max")
        end
    else
        su.saveFile("/etc/depth.cfg", "max")
    end
else
    if args[1] == "max" then
        gpu.setDepth(gpu.maxDepth())
        su.saveFile("/etc/depth.cfg", "max")
    elseif tonumber(args[1]) then
        local depth = tonumber(args[1])
        local ok, err = pcall(gpu.setDepth, depth)
        if not ok then io.stderr:write((err or "unkown") .. "\n") return end
        su.saveFile("/etc/depth.cfg", tostring(depth))
    else
        print("current depth: " .. tostring(math.floor(gpu.getDepth())))
        return
    end
end
os.execute("resetPalette")