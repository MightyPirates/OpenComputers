local su = require("superUtiles")
local fs = require("filesystem")

------------------------------------------

local function create(path)
    if not fs.exists(path) then
        su.saveFile(path, "--regFile")
    end
end

local function isValide(path)
    local file = io.open(path)
    local line = file:read()
    file:close()
    return line == "--regFile"
end

create("/afterUpdate.lua")
create("/beforeUpdate.lua")

------------------------------------------

local lib = {}

function lib.regFile(path, file)
    if not fs.exists(path) then
        error("file not found")
    elseif not isValide(path) then
        error("file is not valide")
    else
        local data = assert(su.getFile(path))
        data = data .. "\nos.execute(\"" .. file .. "\")"
        assert(su.saveFile(path, data))
    end
    return true
end

function lib.unregFile(path, lfile)
    if not fs.exists(path) then
        error("file not found")
    elseif not isValide(path) then
        error("file is not valide")
    else
        local file = assert(io.open(path))
        local strs = {}
        while true do
            local read = file:read()
            if not read then
                break
            end
            if read ~= ("os.execute(\"" .. lfile .. "\")") then
                table.insert(strs, read)
            end
        end
        file:close()
        su.saveFile(path, table.concat(strs, "\n"))
    end
    return true
end

function lib.isReg(path, lfile)
    if not fs.exists(path) then
        error("file not found")
    elseif not isValide(path) then
        error("file is not valide")
    else
        local file = assert(io.open(path))
        while true do
            local read = file:read()
            if not read then
                break
            end
            if read == ("os.execute(\"" .. lfile .. "\")") then
                file:close()
                return true
            end
        end
        file:close()
    end
    return false
end

function lib.checkReg(path, lfile)
    if not lib.isReg(path, lfile) then
        lib.regFile(path, lfile)
        return true
    end
    return false
end

return lib