
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
