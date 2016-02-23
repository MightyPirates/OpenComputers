timers = {}
local nextTimer = 1
local deadline = math.huge
thread = nil

function add(func, time)
    local timer = {
        func = func,
        time = time,
        thread = kernel.modules.threading.currentThread or "kernel",
        next = computer.uptime() + time
    }
    
    if deadline > timer.next then
        deadline = timer.next
        if thread.currentHandler == "yield" then
            thread.currentHandlerArg = deadline
        end
    end
    
    local n = nextTimer
    if timers[n] then
        nextTimer = timers[n].next
    else
        nextTimer = nextTimer + 1
    end
    
    timers[n] = timer
    return n
end

function remove(tid)
    --TODO: rights check
    timers[tid] = {next = nextTimer}
    nextTimer = tid
end

thread = kernel.modules.threading.spawn(function()
    while true do
        local now = computer.uptime()
        for n, timer in ipairs(timers) do
            if timer.thread then
                if timer.next <= now then
                    if type(timer.thread) == "table" then
                        kernel.modules.manageg.protect(timer.thread.sandbox)
                        kernel.modules.threading.currentThread = timer.thread
                    end
                    local res, reason = pcall(timer.func, now)
                    if type(timer.thread) == "table" then
                        kernel.modules.threading.currentThread = thread
                        kernel.modules.manageg.unprotect()
                    end
                    if res then
                        timer.next = now + timer.time
                        if deadline > timer.next then
                            deadline = timer.next
                        end
                    else
                        kernel.io.println("Timer " .. n .. " died: " .. tostring(reason))
                        remove(n)
                    end
                else
                    if deadline > timer.next then
                        deadline = timer.next
                    end
                end
            end
        end
        local dl = deadline
        deadline = math.huge
        coroutine.yield("yield", dl)
    end
end, 0, "[timer]")

