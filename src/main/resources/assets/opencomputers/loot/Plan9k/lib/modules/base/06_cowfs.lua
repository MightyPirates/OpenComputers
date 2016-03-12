local fscount = 0

function new(readfs, writefs)
    if type(readfs) == "string" then
        readfs = component.proxy(component.get(readfs))
    end
    if type(writefs) == "string" then
        writefs = component.proxy(component.get(writefs))
    end
    
    local function getFileFS(path)
        if writefs.exists(path) then
            return writefs
        end
        if not writefs.exists(kernel.modules.vfs.path(path)..".cfsdel."..(kernel.modules.vfs.name(path) or "")) then
            if readfs.exists(path) then
                return readfs
            end
        end
    end
    
    local proxy = {}
    
    proxy.address = "MOOOoooo"
    proxy.spaceUsed = function() return writefs.spaceUsed() end
    proxy.spaceTotal = function() return writefs.spaceTotal() end
    proxy.makeDirectory = function(...) return writefs.makeDirectory(...) end
    proxy.isReadOnly = function() return writefs.isReadOnly() end
    proxy.getLabel = function() return readfs.getLabel() and readfs.getLabel().."-cow" or "cowfs" end
    proxy.setLabel = function() --[[TODO!]] end
    proxy.size = function(path)
        local fs = getFileFS(path)
        if fs then
            return fs.size(path)
        end
        return 0
    end
    proxy.exists = function(path)
        if not writefs.exists(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path)) then
            if readfs.exists(path) then
                return true
            end
        end
        return writefs.exists(path)
    end
    proxy.isDirectory = function(path)
        local fs = getFileFS(path)
        if fs then
            return fs.isDirectory(path)
        end
        return 0
    end
    proxy.rename = function(from, to)
        if getFileFS(to) then
            return false
        end
        local fromfs = getFileFS(from)
        if not fromfs then return false end
        if fromfs == writefs then
            return writefs.rename(from, to)
        elseif fromfs == readfs then
            local rfd = readfs.open(from, "rb")
            local wfd = writefs.open(to, "wb")
            repeat
                local buf = readfs.read(rfd, 1024)
                if buf then
                    if not writefs.write(wfd, buf) then break end
                end
            until not buf
            readfs.close(rfd)
            writefs.close(wfd)
            return true --TODO: handle fails
        end
    end
    proxy.remove = function(path) --TODO: fix directory deletion
        local fs = getFileFS(path)
        if fs == writefs then
            return writefs.remove(path)
        elseif fs == readfs then
            writefs.makeDirectory(kernel.modules.vfs.path(path))
            writefs.close(writefs.open(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path), "w"))
            return true
        end
    end
    proxy.list = function(path)
        local result = {}
        if readfs.isDirectory(path) and not writefs.exists(kernel.modules.vfs.path(path)..".cfsdel."..(kernel.modules.vfs.name(path) or "")) then
            result = readfs.list(path)
        end
        if writefs.isDirectory(path) then
            local wlist = writefs.list(path)
            for _, file in ipairs(wlist) do
                if file:sub(1, 8) == ".cfsdel." then
                    if not writefs.exists(file) then
                        local fn = file:sub(9)
                        for i, f in ipairs(result) do
                            if f:sub(#f) == "/" then f = f:sub(1, #f - 1) end
                            if f == fn then
                                table.remove(result, i)
                                break
                            end
                        end
                    end
                else
                    result[#result + 1] = file
                end
            end
        end
        return result
    end
    proxy.open = function(path, mode) --hnd = orig * 2 [+ 1]
        if mode:sub(1, 1) == "w" then
            if readfs.exists(path) and not writefs.exists(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path)) then
                if readfs.isDirectory(path) then
                   return nil, "Cannot open a directory"
                elseif writefs.isDirectory(kernel.modules.vfs.path(path)) then
                    writefs.close(writefs.open(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path), "w"))
                else
                    return nil, "file not found"
                end
            end
            if not writefs.isDirectory(kernel.modules.vfs.path(path)) then
                return nil, "file not found"
            end
            if writefs.isDirectory(path) then
                return nil, "Cannot open a directory"
            end
            local hnd, err= writefs.open(path, mode)
            if not hnd then
                return hnd, err 
            end
            return hnd * 2
        elseif mode:sub(1, 1) == "a" then
            if readfs.exists(path) and not writefs.exists(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path)) then
                if readfs.isDirectory(path) then
                   return nil, "Cannot open a directory"
                else
                    if not writefs.isDirectory(kernel.modules.vfs.path(path)) then
                        writefs.makeDirectory(kernel.modules.vfs.path(path))
                    end
                    writefs.close(writefs.open(kernel.modules.vfs.path(path)..".cfsdel."..kernel.modules.vfs.name(path), "w"))
                    local rfd = readfs.open(path, "rb")
                    local wfd = writefs.open(path, "wb")
                    repeat
                        local buf = readfs.read(rfd, 1024)
                        if buf then
                            if not writefs.write(wfd, buf) then break end
                        end
                    until not buf
                    readfs.close(rfd)
                    writefs.close(wfd)
                end
            end
            if not writefs.isDirectory(kernel.modules.vfs.path(path)) then
                return nil, "file not found"
            end
            if writefs.isDirectory(path) then
                return nil, "Cannot open a directory"
            end
            local hnd, reason = writefs.open(path, mode)
            return hnd and hnd * 2, reason
        elseif mode:sub(1, 1) == "r" then
            local fs = getFileFS(path)
            if not fs then return nil, "file not found" end
            if fs.isDirectory(path) then
                return nil, "Cannot open a directory"
            end
            local hnd = fs.open(path, mode)
            hnd = hnd * 2
            if fs == readfs then hnd = hnd + 1 end
            return hnd
        end
    end
    proxy.seek = function(h, ...)
        if h % 2 == 0 then
            return writefs.seek(h / 2, ...)
        else
            return readfs.seek((h - 1) / 2, ...)
        end
    end
    proxy.read = function(h, ...)
        if h % 2 == 0 then
            return writefs.read(h / 2, ...)
        else
            return readfs.read((h - 1) / 2, ...)
        end
    end
    proxy.close = function(h, ...)
        if h % 2 == 0 then
            return writefs.close(h / 2, ...)
        else
            return readfs.close((h - 1) / 2, ...)
        end
    end
    proxy.write = function(h, ...)
        if h % 2 == 0 then
            return writefs.write(h / 2, ...)
        else
            return readfs.write((h - 1) / 2, ...)
        end
    end
    return proxy
end