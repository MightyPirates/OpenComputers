function buildDevice(parent, first, size)
    kernel.io.println("Partition from "..first)
    return {
        __type = "f",
        open = function(hnd)
            hnd.pos = 0
            hnd.h = {}
            parent.open(hnd.h)
            parent.seek(hnd.h, "set", first)
        end,
        size = function()
            return size
        end,
        write = function(h, data)
            parent.seek(h.h, "set", first + h.pos)
            parent.write(h.h, data:sub(1, math.min(#data, size - h.pos)))
            
            h.pos = h.pos + #data
            
            return not (h.pos >= size)
        end,
        read = function(h, len)
            len = math.ceil(len)
            if h.pos >= size then
                return
            end
            
            -- 0 1 2 3 4 5 6 7 8
            -- - - - x x x - - -
            
            parent.seek(h.h, "set", first + h.pos)
            local data = parent.read(h.h, math.min(len, size - h.pos))
            
            h.pos = h.pos + len
            return data
        end,
        seek = function(h, whence, offset)
            offset = offset or 0
            if whence == "end" then
                h.pos = math.min(size, math.max(0, size - offset))
                return h.pos
            elseif whence == "set" then
                h.pos = math.min(size, math.max(0, offset))
                return h.pos
            elseif whence == "cur" then
                h.pos = math.min(size, math.max(0, h.pos + offset))
                return h.pos
            else
                error("Invalid whence")
            end
            return math.floor(h.pos)
        end,
        close = function(h)
            if parent.close then
                
            end
        end
    }
end

