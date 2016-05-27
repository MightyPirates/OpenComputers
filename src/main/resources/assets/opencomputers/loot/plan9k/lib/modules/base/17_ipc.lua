function wrap(data, name)
    if type(data) == "table" then
        for k, v in pairs(data) do
            data[k] = wrap(v, k)
        end
        return data
    elseif type(data) == "function" then
        local sandbox = kernel.modules.threading.currentThread.sandbox
        local remThread = kernel.modules.threading.currentThread
        return function(...)
            local thread = kernel.modules.threading.currentThread
            kernel.modules.manageg.protect(sandbox)
            kernel.modules.threading.currentThread = remThread
            local res = {xpcall(data, debug.traceback, ...)}
            kernel.modules.threading.currentThread = thread
            kernel.modules.manageg.unprotect()
            if not res[1] then
                error((tostring(res[2]) or "Unknown IPC error") .. (name and (" on " .. tostring(name)) or ""))
            else
                return table.unpack(res, 2)
            end
        end
    else
        return data
    end
end
