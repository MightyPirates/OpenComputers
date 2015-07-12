proxy = {}
data = {}

proxy.address = "devfs0000"
proxy.spaceUsed = function() return 0 end
proxy.spaceTotal = function() return 0 end
proxy.makeDirectory = function() error("Permission Denied") end
proxy.isReadOnly = function() return true end
proxy.rename = function() error("Permission Denied") end
proxy.remove = function() error("Permission Denied") end
proxy.setLabel = function() error("Permission Denied") end
proxy.seek = function() error("Not supported") end
proxy.size = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        file = file[d]
    end
    return file.size and file.size() or 0
end
proxy.getLabel = function() return "devfs" end

local allocator, handles = kernel.modules.util.getAllocator()

proxy.exists = function()end
proxy.open = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
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
    allocator:unset(handles[h])
end
proxy.write = function(h, ...)
    return handles[h].file.write(handles[h], ...)
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
    for f in pairs(dir) do
        list[#list + 1] = f
    end
    return list
end

data.pts = {}
data.null = {
    __type = "f",
    write = function()end
}
data.zero = {
    __type = "f",
    read = function(h, c)
        c = c or 1
        return ("\0"):rep(c > (2^16) and (2^16) or c)
    end
}
data.random = {
    __type = "f",
    read = function(h, c)
        c = c or 1
        local s = ""
        for i = 1, c do
            s = s .. string.char(math.random(0, 255))
        end
        return s
    end
}

function start()
    kernel.modules.vfs.mount(proxy, "/dev")
end

