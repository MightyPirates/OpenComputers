local fs = require("filesystem")
local unicode = require("unicode")

------------------------------------------

local lib = {}

lib.getPathValue = function(path)
    local path = fs.canonical(path)
    if path:sub(#path, #path) ~= "/" then
        path = path.."/"
    end
    local value = -1
    for i = 1, #path do
        if path:sub(i,i) == "/" then
            value = value + 1
        end
    end
    return value+1
end

lib.rePath = function(path, fspath)
    fspath = fs.canonical(fspath)
    local new = fs.concat(path, fspath)
    if unicode.sub(new, 1, unicode.len(path)) ~= path then new = path end
    return new
end

lib.createFS = function(path, label, classic)
    local path = fs.canonical(path)
    local label = label or "sandbox"
    local classic = classic or false
    local obj = {}

    obj.open = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.open(table.unpack(data))
    end
    obj.exists = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.exists(table.unpack(data))
    end
    obj.isDirectory = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.isDirectory(table.unpack(data))
    end
    obj.lastModified = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.lastModified(table.unpack(data))
    end
    obj.list = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        if not classic then
            return fs.list(table.unpack(data))
        else
            local tab = {}
            for data in fs.list(table.unpack(data)) do
                tab[#tab + 1] = data
            end
            return tab
        end
    end
    obj.makeDirectory = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.makeDirectory(table.unpack(data))
    end
    obj.remove = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.remove(table.unpack(data))
    end
    obj.rename = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        data[2] = lib.rePath(path, data[2])
        return fs.rename(table.unpack(data))
    end
    obj.size = function(...)
        local data = table.pack(...)
        data[1] = lib.rePath(path, data[1])
        return fs.size(table.unpack(data))
    end

    obj.spaceTotal = function()
        return fs.get(path).spaceTotal()
    end
    obj.spaceUsed = function()
        return fs.get(path).spaceUsed()
    end

    obj.getLabel = function() return label end
    obj.setLabel = function(new) local old = label; label = new; return old end
    obj.isReadOnly = function() 
        return fs.get(path).isReadOnly()
    end

    obj.read = function(file, ...) return file:read(...) end
    obj.close = function(file, ...) return file:close(...) end
    obj.seek = function(file, ...) return file:seek(...) end
    obj.write = function(file, ...) return file:write(...) end

    return obj
end

return lib