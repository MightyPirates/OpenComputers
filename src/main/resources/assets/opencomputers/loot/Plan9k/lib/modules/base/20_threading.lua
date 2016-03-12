threads = {}
currentThread = nil

local threadMode = {
    top = 0,
    child = 1
}

local function getPendingThreads()
    local res = {}
    for _, thread in ipairs(threads) do
        if thread.coro and #thread.eventQueue > 0 then
            res[#res + 1] = thread
        end
    end
    return res
end

local function getResumableThreads(threads_)
    local res = {}
    for _,thread in ipairs(threads_) do
        for n,event in ipairs(thread.eventQueue) do
            if event[1] == thread.currentHandler then
                table.remove(thread.eventQueue, n)
                thread.currentEvent = event
                res[#res + 1] = thread
                break
            end
        end
    end
    return res
end

local firstFree = 1
local nextUid = 1

function kill(pid)
    kernel.modules.gc.processkilled(threads[pid])
    threads[pid] = {next = firstFree}
    firstFree = pid
    eachThread(function(thread)
        if thread.coro then
            if thread.currentHandler == "kill" then
                thread.eventQueue[#thread.eventQueue + 1] = {"kill", pid}
            else
                thread.eventQueue[#thread.eventQueue + 1] = {"signal", "kill", pid}
            end
            thread.eventQueue[#thread.eventQueue + 1] = sig
        end
    end)
    --TODO: remove thread timers
end

function spawn(exec, child, name, isthread, _, ...)
    local thread
    local function errprint(text)
        if thread.io_error then
            if not pcall(thread.io_error.write, thread.io_error, tostring(text) .. "\n") then
                kernel.io.println(text)
            end
        else
            kernel.io.println(text)
        end
    end
    
    thread = {
        child = child,
        coro = coroutine.create(function(...)
            local arg = {...}
            local r = {xpcall(function()
                exec(table.unpack(arg))
            end, function(e)
                pcall(errprint, "ERROR IN THREAD " .. thread.pid .. ": " .. tostring(thread.name))
                pcall(errprint, e)
                pcall(errprint, debug.traceback())
                
            end)}
            return table.unpack(r, 2)
        end),
        sandbox = isthread and currentThread.sandbox or kernel.modules.manageg.newsandbox(),
        currentHandler = "arg",
        currentHandlerArg = nil,
        eventQueue = {{"arg", ...}},
        name = name or "unnamed",
        maxPendingSignals = 32,
        maxOpenFiles = 8,
        uid = nextUid,
        parent = currentThread and {pid = currentThread.pid, uid = currentThread.uid},
        cgroups = {
            signal = kernel.modules.cgroups.spawnGroupGetters["signal"](),
            network = kernel.modules.cgroups.spawnGroupGetters["network"](),
            filesystem = kernel.modules.cgroups.spawnGroupGetters["filesystem"](),
            module = kernel.modules.cgroups.spawnGroupGetters["module"](),
            component = kernel.modules.cgroups.spawnGroupGetters["component"](),
        }
    }
    
    nextUid = nextUid + 1
    
    thread.env = currentThread and (isthread and currentThread.env or setmetatable({},{__index=currentThread.env})) or {}
    
    local dest = firstFree
    if threads[firstFree] then
        firstFree = threads[firstFree].next
    else 
        firstFree = firstFree + 1
    end
    thread.pid = dest
    threads[dest] = thread
    
    thread.kill = {
        kill = kill,
        terminate = kill
    }
    
    return thread
end

function countThreadSignals(thread, signal)
    local n = 0
    local first = 0
    for i, sig in ipairs(thread.eventQueue) do
        if sig[1] == signal then
            n = n + 1
            if first < 1 then
                first = i
            end
        end
    end
    return n, first
end

local deadline = math.huge
local pullSignal = computer.pullSignal
computer.pullSignal = function()
    pcall(kernel._println, "Attempted to use non threaded signal pulling")
    pcall(kernel._println, debug.traceback())
    kernel.panic()
end


local function processSignals()
    deadline = math.huge
    for _, thread in ipairs(threads) do
        if (thread.currentHandler == "yield" or thread.currentHandler == "signal")
                and deadline > (tonumber(thread.currentHandlerArg) or math.huge) then
            deadline = thread.currentHandlerArg or math.huge
        end
    end
    --kernel.io.println("Deadline: "..(deadline - computer.uptime()))
    local sig = {"signal", pullSignal(deadline - computer.uptime())}
    
    for _, thread in ipairs(threads) do
        if thread.coro then
            local nsig, oldest = countThreadSignals(thread, "signal")
            if nsig > thread.maxPendingSignals then --TODO: make it a bit more intelligent
                table.remove(thread.eventQueue, oldest)
            end
            if thread.currentHandler == "yield" then
                --kernel.io.println("yield ck: "..tostring((thread.currentHandlerArg or math.huge) - computer.uptime()))
                if (thread.currentHandlerArg or math.huge) <= computer.uptime() then
                    thread.eventQueue[#thread.eventQueue + 1] = {"yield"}
                end
            end
            if thread.cgroups.signal.global then
                thread.eventQueue[#thread.eventQueue + 1] = sig
            end
        end
    end
end

function eachThread(func)
    for _, thread in ipairs(threads) do
        if thread.coro then
            func(thread)
        end
    end
end

local lastYield = computer.uptime()

function checkTimeout()
    local uptime = computer.uptime()
    
    if uptime - lastYield > 3 then
        return true
    end
    return false
end

function start()
    while true do
        -- pending = getPendingThreads()
        --local resumable = getResumableThreads(pending)
        
        local pending = {}
        for _, thread in ipairs(threads) do
            if thread.coro and #thread.eventQueue > 0 then
                pending[#pending + 1] = thread
            end
        end
        
        local resumable = {}
        for _,thread in ipairs(pending) do
            for n,event in ipairs(thread.eventQueue) do
                if event[1] == thread.currentHandler then
                    table.remove(thread.eventQueue, n)
                    thread.currentEvent = event
                    resumable[#resumable + 1] = thread
                    break
                end
            end
        end
        
        lastYield = computer.uptime()
        while #resumable > 0 do
            for _, thread in ipairs(resumable) do
                --kernel.io.println("Resume " .. tostring(thread.name) .. " with " 
                --    .. tostring(type(thread.currentEvent) == "table" and thread.currentEvent[1] or "unknown")
                --    ..(thread.currentEvent[2] and (", " .. tostring(thread.currentEvent[2])) or ""))
                        
                kernel.modules.manageg.protect(thread.sandbox)
                currentThread = thread
                local state, reason, arg = coroutine.resume(thread.coro, table.unpack(thread.currentEvent, 2))
                currentThread = nil
                kernel.modules.manageg.unprotect()
                
                if not state or coroutine.status(thread.coro) == "dead" then
                    kill(thread.pid)
                    if reason then
                        kernel.io.println("Thread " .. tostring(thread.name) .. "(" .. tostring(thread.pid) .. ") dead: "
                            .. tostring(reason or "unknown/done") .. ", after "
                            .. tostring(type(thread.currentEvent) == "table" and thread.currentEvent[1] or "unknown"))
                    end
                else
                    thread.currentEvent = nil
                    thread.currentHandler = reason
                    thread.currentHandlerArg = arg
                end
            end
            if checkTimeout() then
                break
            end
            pending = getPendingThreads()
            resumable = getResumableThreads(pending)
        end
        processSignals()
    end
end


