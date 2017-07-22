local unicode = require("unicode")
local shell = require("shell")

local args, options = shell.parse(...)

local bytes = 0
local chars = 0
local words = 0
local lines = 0

local word = false
while true do
    local data = io.read(1)
    if not data then break end
    if io.input().remaining() > 0 then
        data = data .. io.read(io.input().remaining())
    end
    bytes = bytes + #data
    chars = chars + unicode.len(data)
    for char in data:gmatch(".") do
        if char == "\n" then
            lines = lines + 1
        end
        if data:match("%s") and word then
            word = false
            words = words + 1
        else
            word = true
        end
    end
end

if options.c or options.bytes then
    print(bytes)
elseif options.m or options.chars then
    print(chars)
elseif options.l or options.lines then
    print(lines)
else
    print(words)
end
