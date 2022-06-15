local serialization = require("serialization")
local fs = require("filesystem")
local su = require("superUtiles")
local shell = require("shell")

local args, options = shell.parse(...)
if #args == 0 then
    print("Usage:")
    print("modLoader program mods... / args...")
    return
end

local programPath = shell.resolve(args[1], "lua")
if not programPath or not fs.exists(programPath) then io.stderr:write("programm is not found") return end
if fs.isDirectory(programPath) then io.stderr:write("programm is directory") return end

local programmCode = assert(su.getFile(programPath))

local mods = {}
local programArgs = {}
local programArgsFlag = false

for i = 2, #args do
    local v = args[i]
    if v == "/" and not programArgsFlag then
        programArgsFlag = true
    elseif programArgsFlag then
        table.insert(programArgs, v)
    else
        local modPath = shell.resolve(v)
        if not modPath or not fs.exists(modPath) then
            io.stderr:write("mod " .. v .. " not found")
            return
        elseif fs.isDirectory(modPath) then
            io.stderr:write("mod " .. v .. " is directory")
            return
        end

        table.insert(mods, assert(serialization.unserialize(assert(su.getFile(modPath)))))
    end
end

do
    local newmods = {}
    for i = #mods, 1 do
        table.insert(newmods, mods[i])
    end
    mods = newmods
end

--------------------------------------------

local programmEnv = {}
setmetatable(programmEnv, {__index = function(tbl, key)
    return _G[key]
end})

local function getStructure(mod, name, skip, standart)
    local struct = mod[name]
    if not struct then struct = mod["/" .. name] end
    if not struct and not skip then
        error("no struct " .. name .. " in mod " .. (getStructure(mod, "name") or "noname"))
    end
    return struct or standart
end

local function runStructure(mod, name, skip, standart, ...)
    local struct = getStructure(mod, name, skip, standart)
    return table.unpack({assert(xpcall(assert(load(struct, "=struct", nil, programmEnv)), debug.traceback, ...))}, 2)
end

--------------------------------------------

for i = 1, #mods do
    local mod = mods[i]
    programmCode = assert(runStructure(mod, "codeModifier", true, "return ({...})[1]", programmCode, mod, programArgs, programPath))
    assert(runStructure(mod, "preLoader", true, "return true", programmCode, mod, programArgs, programPath))
end

--------------------------------------------

local function runProgramm(...)
    os.setenv("_", programPath)
    return table.unpack({assert(xpcall(assert(load(programmCode, "=" .. programPath, nil, programmEnv)), debug.traceback, ...))}, 2)
end

return runProgramm(...)