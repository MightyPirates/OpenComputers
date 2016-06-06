function split(str, pat)
    local t = {}
    if type(pat) == "number" then
        while #str > 0 do
            table.insert(t, str:sub(1, pat))
            str = str:sub(pat + 1)
        end
    else
        local fpat = "(.-)" .. pat
        local last_end = 1
        local s, e, cap = str:find(fpat, 1)
        while s do
            if s ~= 1 or cap ~= "" then
            table.insert(t, cap)
            end
            last_end = e+1
            s, e, cap = str:find(fpat, last_end)
        end
        if last_end <= #str then
            cap = str:sub(last_end)
            table.insert(t, cap)
        end
    end
    return t
end

--------

function getAllocator()
    local allocator = {next = 1}
    local list = {}
    function allocator:get()
        local n = self.next
        self.next = (list[n] and list[n].next) or (#list + 2)
        list[n] = {id = n}
        return list[n]
    end
    
    function allocator:unset(e)
        local eid = e.id
        list[eid] = {next = self.next}
        self.next = eid
        return list[n]
    end
    return allocator, list
end

--------

function uuidBin(uuid)
    local undashed = uuid:gsub("-","")
    local high = tonumber(undashed:sub(1,16), 16)
    local low = tonumber(undashed:sub(17), 16)
    return string.pack(">LL",  high, low)
end

function binUUID(uuid)
    local raw = toHex(uuid)
    return raw:sub(1, 8) .. "-" .. raw:sub(9, 12) .. "-" .. raw:sub(13, 16) .. "-" .. raw:sub(17, 20) .. "-" .. raw:sub(21)
end
    
--------

--------

local function sixteen(val)
    if val < 10 then
        return string.char(48 + val)
    else
        return string.char(97 + val - 10)
    end
end

function toHex(bin)
    local res = ""
    for i = 1, #bin do
        local byte = bin:byte(i)
        res = res .. sixteen(byte >> 4) .. sixteen(byte & 0x0F)
    end
    return res
end