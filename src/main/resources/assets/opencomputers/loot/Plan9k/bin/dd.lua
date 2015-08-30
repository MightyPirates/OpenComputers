local shell = require "shell"
local filesystem = require "filesystem"

local args = {...}
local options = {}
options.count = math.huge
options.bs = 1
options["if"] = "-"
options.of = "-"

for _, arg in pairs(args) do
    local k, v = arg:match("(%w+)=(.+)")
    if not k then
        print("Ilegal argument: " .. arg)
        return
    end
    options[k] = v
end

if type(options.count) == "string" then options.count = tonumber(options.count) or math.huge end
if type(options.bs) == "string" then options.bs = tonumber(options.bs) or 1 end

local reader
local writer

if options["if"] == "-" then
    reader = {
        read = io.read,
        close = function()end
    }
else
    local inHnd = filesystem.open(options["if"], "r")
    reader = {
        read = function(...)return inHnd:read(...)end,
        close = function()inHnd:close()end
    }
end

if options.of == "-" then
    io.output():setvbuf("full", options.bs)
    writer = {
        write = function(data)return io.write(data) end,
        close = function()io.output():setvbuf("no")end
    }
else
    local outHnd = filesystem.open(options.of, "w")
    writer = {
        write = function(data)return outHnd:write(data) and #data end,
        close = function()outHnd:close()end
    }
end

local start = computer.uptime()
local dcount = 0

for n = 1, options.count do
    local data = reader.read(options.bs)
    if not data then
        print("End of input")
        break
    end
    local wrote = writer.write(data)
    dcount = dcount + (wrote and options.bs or 0)
    if not wrote then
        print("Output full")
        break
    end
    if options.wait then
        os.sleep(tonumber(options.wait))
    else
        os.sleep(0)
    end
end

local time = computer.uptime() - start

reader.close()
writer.close()

print(dcount .. " bytes (" .. (dcount / 1024) .. " KB) copied, " .. time .. "s, " .. (dcount / time / 1024) .. " KB/s")

