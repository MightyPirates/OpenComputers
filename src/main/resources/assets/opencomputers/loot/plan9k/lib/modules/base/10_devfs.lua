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

data.pts = {}
data.null = {
    __type = "f",
    write = function()end
}
data.kmsg = {
    __type = "f",
    write = function(h, data)
        kernel.io.println(data)
    end
}
data.kcmd = {
    __type = "f",
    open = function(h) h.buf = "" end,
    write = function(h, data)
        h.buf = h.buf .. kernel.modules.cmd.execute(data) or ""
    end,
    read = function(h)
        local res = h.buf
        h.buf = ""
        return res
    end,
    close = function(h) h.buf = nil end
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

