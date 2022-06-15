local serialization = require("serialization")
local fs = require("filesystem")
local su = require("superUtiles")
local unicode = require("unicode")

----------------------------------------------------

local lib = {}

function lib.simpleUnpack(mainpath, data)
    for path, data in pairs(data) do
        interrupt()
        path = fs.concat(mainpath, path)
        fs.makeDirectory(fs.path(path))
        su.saveFile(path, data)
    end
end

function lib.simplePack(mainpath)
    if unicode.sub(mainpath, unicode.len(mainpath), unicode.len(mainpath)) ~= "/" then mainpath = mainpath .. "/" end
    local files = {}
    local function getFolderData(path)
        interrupt()
        for data in fs.list(path) do
            interrupt()
            local fullPath = fs.concat(path, data)
            if fs.isDirectory(fullPath) then
                getFolderData(fullPath)
            else
                files[unicode.sub(fullPath, unicode.len(mainpath), unicode.len(fullPath))] = su.getFile(fullPath)
            end
        end
    end
    getFolderData(mainpath)
    return files
end

--------------------------

function lib.pack(mainpath)
    local LibDeflate = require("LibDeflate")
    local files = lib.simplePack(mainpath)
    local raw_data = serialization.serialize(files)
    local compressData = LibDeflate.CompressDeflate("", raw_data)
    return compressData
end

function lib.unpack(mainpath, data)
    local LibDeflate = require("LibDeflate")
    data = LibDeflate.DecompressDeflate("", data)
    data = serialization.unserialize(data)
    lib.simpleUnpack(mainpath, data)
end

function lib.packPro(mainpath, filepath, compress)
    local files = lib.simplePack(mainpath)
    local raw_data = serialization.serialize(files)
    local compressData = raw_data
    if compress then
        local LibDeflate = require("LibDeflate")
        compressData = LibDeflate.CompressDeflate("", raw_data)
    end
    return assert(su.saveFile(filepath, compressData))
end

function lib.unpackPro(mainpath, filepath, compress)
    local data = assert(su.getFile(filepath))
    if compress then
        local LibDeflate = require("LibDeflate")
        data = LibDeflate.DecompressDeflate("", data)
    end
    lib.simpleUnpack(mainpath, assert(serialization.unserialize(data)))
    return true
end

return lib