listeners = {}

function listen(signal, listener)
    listeners[signal] = listeners[signal] or {}
    listeners[signal][#listeners[signal] + 1] = listener
end

function start()
    thread = kernel.modules.threading.spawn(function()
        while true do
            local sig = {coroutine.yield("signal", dl)}
            if listeners[sig[1]] then
                for _, listener in pairs(listeners[sig[1]]) do
                    --pcall(kernel.io.println, "KEVD: "..sig[1])
                    xpcall(listener, function(e)
                        kernel.io.println("keventd error("..tostring(sig[1]).."): "..tostring(e))
                        pcall(kernel.io.println, debug.traceback())
                    end, table.unpack(sig))
                end
            end
        end
    end, 0, "[keventd]")
    setmetatable(thread.env, {__index = kernel.modules.init.thread.env})
end
