local su = require("superUtiles")
local fs = require("filesystem")
local serialization = require("serialization")

-------------------------------------------------

local typesPath = "/free/trashData/types"
local valuesPath = "/free/trashData/values"

fs.makeDirectory(typesPath)
fs.makeDirectory(valuesPath)

-------------------------------------------------

local lib = {}

function lib.crypto(value)
    local vtype = type(value)
    if vtype ~= "number" and vtype ~= "string" and vtype ~= "table" and vtype ~= "nil" and vtype ~= "boolean" then
        error("unsupported type: " .. vtype)
        return nil --на всякий пожарный
    end

    if vtype == "table" then
        return assert(serialization.serialize(value)), vtype
    elseif vtype == "string" then
        return value, vtype
    else
        return tostring(value), vtype
    end
end

function lib.uncrypto(str, vtype)
    if vtype == "number" then
        return tonumber(str)
    elseif vtype == "table" then
        return assert(serialization.unserialize(str))
    elseif vtype == "string" then
        return str
    elseif vtype == "nil" then
        return nil
    elseif vtype == "boolean" then
        return str == "true" and true or false
    end
end

-------------------------------------------------

function lib.pull(key)
    if fs.exists(fs.concat(valuesPath, key)) then
        return lib.uncrypto(assert(su.getFile(fs.concat(valuesPath, key))), assert(su.getFile(fs.concat(typesPath, key))))
    else
        return nil
    end
end

function lib.push(key, data)
    local str, vtype = lib.crypto(data)
    assert(su.saveFile(fs.concat(valuesPath, key), str))
    assert(su.saveFile(fs.concat(typesPath, key), vtype))
    return true
end

function lib.pullPlus(key, originalValue)
    local data = lib.pull(key)
    if not data then
        if originalValue then
            lib.push(key, originalValue)
            data = lib.pull(key)
        end
    end
    return data
end

function lib.pullData(key, originalValue)
    local obj = {}

    function obj.pull()
        return lib.pullPlus(key, originalValue)
    end

    function obj.push(newdata)
        return lib.push(key, newdata)
    end

    return obj, obj.pull()
end

return lib