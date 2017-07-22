local metatable = {__index = kernel._K, __newindex=function()end}

function start()
    metatable.__index = kernel.userspace
    setmetatable(kernel._G, metatable)
end

function newsandbox()
    local sandbox = {}
    sandbox._G = sandbox
    sandbox.__STRICT = false
    
    local mt = {}
    mt.__declared = {}
    
    mt.__index = function(t, n)
        local res = kernel.userspace[n]
        if res then
            return res
        end
        if sandbox.__STRICT and (not mt.__declared[n]) then
            error("variable '"..n.."' is not declared", 2)
        end
        return rawget(t, n)
    end
    
    mt.__newindex = function(t, n, v)
        if sandbox.__STRICT and (not mt.__declared[n]) then
            error("assign to undeclared variable '"..n.."'", 2)
        end
        rawset(t, n, v)
    end
    
    return setmetatable(sandbox, mt)
end

--User function. Defines globals for strict mode.
function global(sandbox, ...)
   for _, v in ipairs{...} do getmetatable(mt).__declared[v] = true end
end


local protectionStack = {{newindex = function()end, index = kernel.userspace}}

function protect(sandbox)
    table.insert(protectionStack, {
        newindex = metatable.__newindex,
        index = metatable.__index
    })
    metatable.__newindex = sandbox
    metatable.__index = sandbox
end

function unprotect()
    local prot = table.remove(protectionStack, protectionStack.n)
    metatable.__newindex = prot.newindex
    metatable.__index = prot.index
end