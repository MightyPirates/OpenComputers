local su = require("superUtiles")
local fs = require("filesystem")
local regFile = require("regFile")

-------------------------------------------------

local unloadTwicks = [[
    local su = require("superUtiles")
    local fs = require("filesystem")
    local trashData = require("trashData")

    local twicksPath = "/usr/twicks"
    local twicksFlagsPath = "/free/twicks/active"
    fs.makeDirectory(twicksPath)

    local function isEnabled(name)
        return fs.exists(fs.concat(twicksFlagsPath, name))
    end

    local function getError(err)
        return err or "unknown error"
    end
    
    local function disable(name)
        if not isEnabled(name) then return end

        local func, err = loadfile(fs.concat(twicksPath, name))
        if not func then
            error("error from load twick: " .. getError(err))
            return
        end
        local ok, err = pcall(func, false)
        if not ok then
            error("error to run twick: " .. getError(err))
            return
        end
        fs.remove(fs.concat(twicksFlagsPath, name))
        return true
    end

    local function twicks(state)
        local tbl = {}
        if state == nil then
            for file in fs.list(twicksPath) do
                table.insert(tbl, file)
            end
        elseif state == true then
            for file in fs.list(twicksPath) do
                local fullPath = fs.concat(twicksFlagsPath, file)
                if fs.exists(fullPath) then
                    table.insert(tbl, file)
                end
            end
        elseif state == false then
            for file in fs.list(twicksPath) do
                local fullPath = fs.concat(twicksFlagsPath, file)
                if not fs.exists(fullPath) then
                    table.insert(tbl, file)
                end
            end
        else
            error("unsupported mode")
        end
    
        return tbl
    end

    local tbl = trashData.pullPlus("enablesTwicks", {})

    for i, v in ipairs(twicks(true)) do
        disable(v)
        table.insert(tbl, v)
        trashData.push("enablesTwicks", tbl)
    end
]]

local loadTwicks = [[
    local su = require("superUtiles")
    local fs = require("filesystem")
    local trashData = require("trashData")

    local twicksPath = "/usr/twicks"
    local twicksFlagsPath = "/free/twicks/active"
    fs.makeDirectory(twicksPath)

    local function isEnabled(name)
        return fs.exists(fs.concat(twicksFlagsPath, name))
    end

    local function getError(err)
        return err or "unknown error"
    end
    
    local function enable(name)
        if isEnabled(name) then return end
    
        local func, err = loadfile(fs.concat(twicksPath, name))
        if not func then
            error("error from load twick: " .. getError(err))
            return
        end
        local ok, err = pcall(func, true)
        if not ok then
            error("error to run twick: " .. getError(err))
            return
        end
        su.saveFile(fs.concat(twicksFlagsPath, name), "")
        return true
    end

    local function twicks(state)
        local tbl = {}
        if state == nil then
            for file in fs.list(twicksPath) do
                table.insert(tbl, file)
            end
        elseif state == true then
            for file in fs.list(twicksPath) do
                local fullPath = fs.concat(twicksFlagsPath, file)
                if fs.exists(fullPath) then
                    table.insert(tbl, file)
                end
            end
        elseif state == false then
            for file in fs.list(twicksPath) do
                local fullPath = fs.concat(twicksFlagsPath, file)
                if not fs.exists(fullPath) then
                    table.insert(tbl, file)
                end
            end
        else
            error("unsupported mode")
        end
    
        return tbl
    end

    local tbl = trashData.pullPlus("enablesTwicks", {})

    for i, v in ipairs(tbl) do
        enable(v)
    end

    trashData.push("enablesTwicks", nil)
]]

if not fs.exists("/usr/twickMenager/unload.lua") then
    su.saveFile("/usr/twickMenager/unload.lua", unloadTwicks)
end

if not fs.exists("/usr/twickMenager/load.lua") then
    su.saveFile("/usr/twickMenager/load.lua", loadTwicks)
end

regFile.checkReg("/beforeUpdate.lua", "/usr/twickMenager/unload.lua")
regFile.checkReg("/afterUpdate.lua", "/usr/twickMenager/load.lua")

-------------------------------------------------

local twicksPath = "/usr/twicks"
local twicksFlagsPath = "/free/twicks/active"
fs.makeDirectory(twicksPath)

-------------------------------------------------

local function getError(err)
    return err or "unknown error"
end

-------------------------------------------------

local lib = {}

function lib.twicks(state)
    local tbl = {}
    if state == nil then
        for file in fs.list(twicksPath) do
            table.insert(tbl, file)
        end
    elseif state == true then
        for file in fs.list(twicksPath) do
            local fullPath = fs.concat(twicksFlagsPath, file)
            if fs.exists(fullPath) then
                table.insert(tbl, file)
            end
        end
    elseif state == false then
        for file in fs.list(twicksPath) do
            local fullPath = fs.concat(twicksFlagsPath, file)
            if not fs.exists(fullPath) then
                table.insert(tbl, file)
            end
        end
    else
        error("unsupported mode")
    end

    return tbl
end

function lib.isEnabled(name)
    return fs.exists(fs.concat(twicksFlagsPath, name))
end

function lib.isInstalled(name)
    return fs.exists(fs.concat(twicksPath, name))
end

function lib.enable(name)
    if lib.isEnabled(name) then return end

    local func, err = loadfile(fs.concat(twicksPath, name))
    if not func then
        error("error from load twick: " .. getError(err))
        return
    end
    local ok, err = pcall(func, true)
    if not ok then
        error("error to run twick: " .. getError(err))
        return
    end
    su.saveFile(fs.concat(twicksFlagsPath, name), "")
    return true
end

function lib.disable(name)
    if not lib.isEnabled(name) then return end

    local func, err = loadfile(fs.concat(twicksPath, name))
    if not func then
        error("error from load twick: " .. getError(err))
        return
    end
    local ok, err = pcall(func, false)
    if not ok then
        error("error to run twick: " .. getError(err))
        return
    end
    fs.remove(fs.concat(twicksFlagsPath, name))
    return true
end

function lib.remove(name)
    if lib.isEnabled(name) then
        if lib.disable(name) then
            fs.remove(fs.concat(twicksPath, name))
            return true --твик был выключен а не просто удален
        end
    else
        fs.remove(fs.concat(twicksPath, name))
    end
end

function lib.insert(name, data)
    if not fs.exists(fs.concat(twicksPath, name)) then
        su.saveFile(fs.concat(twicksPath, name), data)
        return true
    else
        return nil, "this twick installed"
    end
end

function lib.changeState(name)
    if lib.isEnabled(name) then
        return lib.disable(name)
    else
        return lib.enable(name)
    end
end

return lib