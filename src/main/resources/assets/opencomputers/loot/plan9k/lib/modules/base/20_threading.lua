threads = {}
currentThread = nil
eventFilters = {signal = {}}

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

function eachThread(func)
    for _, thread in ipairs(threads) do
        if thread.coro then
            func(thread)
        end
    end
end

--------------

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
        deadline = computer.uptime(),
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
    
    kernel.io.debug("Spawn thread " .. tostring(name))
    
    return thread
end

---

local function getPendingThreads()
    local res = {}
    for _, thread in ipairs(threads) do
        if thread.coro then
            res[#res + 1] = thread
        end
    end
    return res
end

local function getResumableThreads(threads_)
    local res = {}
    for _,thread in ipairs(threads_) do
        thread.currentEvent = nil
        for n,event in ipairs(thread.eventQueue) do
            if event[1] == thread.currentHandler then
                table.remove(thread.eventQueue, n)
                thread.currentEvent = event
                res[#res + 1] = thread
                break
            end
        end
        if not thread.currentEvent and thread.deadline <= computer.uptime() then
            thread.currentEvent = {"timeout"}
            res[#res + 1] = thread
        end
    end
    return res
end

local function processSignals()
    deadline = math.huge
    for _, thread in ipairs(threads) do
        if deadline > (thread.deadline or math.huge) then
            if not thread.deadline then
                kernel.io.println("Nil deadline for " .. thread.name .. " on " .. tostring(thread.currentHandler))
            end
            deadline = thread.deadline
        end
    end
    --kernel.io.debug("Pull deadline: "..(deadline - computer.uptime()))
    local sig = {"signal", pullSignal(deadline - computer.uptime())}
    
    local function filt(f, signal, ...)
        if not signal then
            return true
        end
        if type(f[signal]) == "table" then
            return filt(f[signal], ...)
        end
        return (not f[signal]) and true or (function(...)
            local s, e = xpcall(f[signal], function(err)
                    kernel._println("Signal filter error:")
                    kernel._println(tostring(e))
                    kernel._println(debug.traceback())
                    kernel.panic("Signal filtering failed")
                end, ...)
            return s and e
        end)(...)
    end
    if not filt(eventFilters, table.unpack(sig)) then
        sig = {}
    end
    
    for _, thread in ipairs(threads) do
        if thread.coro then
            --[[if thread.currentHandler == "yield" then
                --kernel.io.println("yield ck: "..tostring((thread.currentHandlerArg or math.huge) - computer.uptime()))
                if (thread.currentHandlerArg or math.huge) <= computer.uptime() then
                    thread.eventQueue[#thread.eventQueue + 1] = {"yield"}
                end
            end]]--
            if thread.cgroups.signal.global and sig[2] then
                local nsig, oldest = countThreadSignals(thread, "signal")
                if nsig > thread.maxPendingSignals then --TODO: make it a bit more intelligent
                    table.remove(thread.eventQueue, oldest)
                end
            
                thread.eventQueue[#thread.eventQueue + 1] = sig
            end
        end
    end
end

----

local lastYield = computer.uptime()

yieldTime = 3
function checkTimeout()
    local uptime = computer.uptime()
    
    if uptime - lastYield > yieldTime then
        return true
    end
    return false
end

function start()
    while true do
        local pending = getPendingThreads()
        local resumable = getResumableThreads(pending)
        
        lastYield = computer.uptime()
        while #resumable > 0 do
            for _, thread in ipairs(resumable) do
                --kernel.io.println("Resume " .. tostring(thread.name) .. " with " 
                --    .. tostring(type(thread.currentEvent) == "table" and thread.currentEvent[1] or "unknown")
                --    ..(thread.currentEvent[2] and (", " .. tostring(thread.currentEvent[2])) or ""))
                
                thread.deadline = math.huge
                kernel.modules.manageg.protect(thread.sandbox)
                currentThread = thread
                local state, reason, arg = coroutine.resume(thread.coro, table.unpack(thread.currentEvent, 2))
                currentThread = nil
                kernel.modules.manageg.unprotect()
                
                if not state or coroutine.status(thread.coro) == "dead" then
                    kill(thread.pid)
                    if reason then
                        kernel.io.println("Thread " .. tostring(thread.name) .. "(" .. tostring(thread.pid) .. ") died: "
                            .. tostring(reason or "unknown/done") .. ", after "
                            .. tostring(type(thread.currentEvent) == "table" and thread.currentEvent[1] or "unknown"))
                    end
                else
                    --kernel.io.println("Yield arg from " .. tostring(thread.name) .. ": " .. tostring(arg))
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


