allocator, list = kernel.modules.util.getAllocator()

function new()
    local pty = allocator:get()
    pty.mi, pty.so = kernel.modules.buffer.pipe()
    pty.si, pty.mo = kernel.modules.buffer.pipe()
    
    function pty:read(...)
        return self.si:read(...)
    end
    
    function pty:write(...)
        return self.so:write(...)
    end
    
    return pty.id, pty.mi, pty.mo, pty.si, pty.so
end

function nextPty(at)
    local pty = at and tonumber(at) + 1 or 1
    if not list[pty] then
        return
    elseif list[pty].next then
        return nextPty(pty)
    else
        return pty, {
            __type = "f",
            read = function(h, ...)
                return list[pty]:read(...)
            end,
            write = function(h, ...)
                return list[pty]:write(...)
            end
        }
    end
end

function start()
    setmetatable(kernel.modules.devfs.data.pts, {
        __newindex = function()error("Access denied")end,
        __index = function(_,k)
            if list[tonumber(k)] and list[tonumber(k)].id then
                return {
                    __type = "f",
                    read = function(h, ...)
                        return list[tonumber(k)]:read(...)
                    end,
                    write = function(h, ...)
                        return list[tonumber(k)]:write(...)
                    end
                    }
            end
        end,
        __pairs = function()
            local lastIndex = nil
            return function()
                local k, v = nextPty(lastIndex)
                lastIndex = k
                if not k then return end
                return k and tostring(k), k and v
            end
        end
    })
end
