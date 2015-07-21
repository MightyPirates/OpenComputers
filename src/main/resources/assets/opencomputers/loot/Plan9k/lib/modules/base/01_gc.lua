local onshutdown = {}
local onprocessdead = {}

function onShutdown(callback)
    if type(callback) ~= "function" then
        error("GC callback is not a function")
    end
    onshutdown[#onshutdown + 1] = callback
end

function shutdown(...)
    for _, callback in ipairs(onshutdown) do
        pcall(callback)
    end
    computer.shutdown(...)
end

function onProcessKilled(callback)
    if type(callback) ~= "function" then
        error("GC callback is not a function")
    end
    onprocessdead[#onprocessdead + 1] = callback
end

function processkilled(...)
    for _, callback in ipairs(onprocessdead) do
        pcall(callback, ...)
    end
end
