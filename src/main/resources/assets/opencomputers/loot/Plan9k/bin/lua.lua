local serialization = require("serialization")
local term = require("term")
local fs = require("filesystem")

local args = {...}

local env = setmetatable({}, {__index = _ENV})

local function optrequire(...)
    local success, module = pcall(require, ...)
    if success then
        return module
    end
end

if args[1] and fs.exists(args[1]) then --non standard, require -i !!!
    local f = io.open(args[1])
    local code = load(f:read("*all"), "="..args[1], "t", env)
    f:close()
    xpcall(code, debug.traceback)
end

local hist = {}
while true do
    io.write(tostring(env._PROMPT or "lua> "))
    local command = term.read(hist)
    local code, reason
    if string.sub(command, 1, 1) == "=" then
        code, reason = load("return " .. string.sub(command, 2), "=stdin", "t", env)
    else
        code, reason = load(command, "=stdin", "t", env)
    end
    
    if code then
        local result = table.pack(xpcall(code, debug.traceback))
        if not result[1] then
            if type(result[2]) == "table" and result[2].reason == "terminated" then
                os.exit(result[2].code)
            end
            io.stderr:write(tostring(result[2]) .. "\n")
        else
            for i = 2, result.n do
                io.write(serialization.serialize(result[i], true) .. "\n")
            end
        end
    else
        io.stderr:write(tostring(reason) .. "\n")
    end
end
