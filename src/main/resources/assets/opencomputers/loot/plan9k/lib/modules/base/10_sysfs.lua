proxy = {}
data = {}

proxy.address = "sysfs0000"
proxy.spaceUsed = function() return 0 end
proxy.spaceTotal = function() return 0 end
proxy.makeDirectory = function() error("Permission Denied") end
proxy.isReadOnly = function() return true end
proxy.rename = function() error("Permission Denied") end
proxy.remove = function() error("Permission Denied") end
proxy.setLabel = function() error("Permission Denied") end
proxy.size = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        file = file[d]
    end
    return file.size and file.size() or 0
end
proxy.getLabel = function() return "sysfs" end

local allocator, handles = kernel.modules.util.getAllocator()

proxy.exists = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        if not file[d] then
            return false
        end
        file = file[d]
    end
    return file and true or false
end
proxy.open = function(path)
    kernel.io.debug("Sysfs open: " .. tostring(path))
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        if not file[d] then
            return nil, "File not found"
        end
        file = file[d]
    end
    local hnd = allocator:get()
    hnd.file = file
    if hnd.file.open then
        hnd.file.open(hnd)
    end
    return hnd.id
end
proxy.read = function(h, ...)
    return handles[h].file.read(handles[h], ...)
end
proxy.close = function(h)
    if handles[h].file.close then
        handles[h].file.close(handles[h])
    end
    allocator:unset(handles[h])
end
proxy.write = function(h, ...)
    return handles[h].file.write(handles[h], ...)
end
proxy.seek = function(h, ...)
    return handles[h].file.seek(handles[h], ...)
end
proxy.isDirectory = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local dir = data
    for _, d in pairs(seg) do
        dir = dir[d]
    end
    if dir.__type then
        return false
    end
    return true
end
proxy.list = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local dir = data
    for _, d in pairs(seg) do
        dir = dir[d]
    end
    if dir.__type then
        error("File is not a directory")
    end
    local list = {}
    for f, node in pairs(dir) do
        list[#list + 1] = f .. (node.__type and "" or "/")
    end
    return list
end

-----

function roFile(data)
    return {
        __type = "f",
        read = function(h)
            if h.read then
                return nil
            end
            h.read = true
            return (type(data) == "function") and data() or tostring(data)
        end
    }
end

data.net = {}

function start()
    kernel.modules.vfs.mount(proxy, "/sys")
end
