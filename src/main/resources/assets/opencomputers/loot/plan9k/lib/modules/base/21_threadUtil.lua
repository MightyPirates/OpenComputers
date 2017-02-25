function joinThread(pid)
    --coroutine.yield("yield", 0)
    while true do
        local dead = coroutine.yield("kill")
        if pid == dead then
            break
        end
    end
end

function getThreadInfo()
    local info = {}
    kernel.modules.threading.eachThread(function(thread)
        info[thread.pid] = {
            pid = thread.pid,
            uid = thread.uid,
            name = thread.name,
            parent = thread.parent and thread.parent.pid or nil
        }
    end)
    return info
end

function userKill(pid, signal, ...)
    if not kernel.modules.threading.threads[pid]
      or not kernel.modules.threading.threads[pid].coro then
        return nil, "Thread does not exists"
    end
    if not kernel.modules.threading.threads[pid].kill[signal] then
        return nil, "Unknown signal"
    end
    local args = {...}
    local thread = kernel.modules.threading.threads[pid]
    kernel.modules.manageg.protect(thread.sandbox)
    --TODO: probably set threading.currentThread here
    local res, reason = pcall(function()
        thread.kill[signal](table.unpack(args))
    end)
    kernel.modules.manageg.unprotect()
    if not res then
        kernel.modules.threading.kill(pid)
    end
    return true
end

function select(timeout, ...)
    checkArg(1, timeout, "number")
    local funcs = {}
    for n, f in ipairs(...) do
        checkArg(n + 1, f, "function")
        funcs[n] = coroutine.create(f)
    end
    
end

function setKillHandler(signal, handler) --WAT
    if not kernel.modules.threading.threads[pid]
      or not kernel.modules.threading.threads[pid].coro then
        return nil, "Thread does not exists"
    end
    if signal == "kill" then
        return nil, "Cannot override kill"
    end
    kernel.modules.threading.threads[pid].kill[signal] = handler
    return true
end
