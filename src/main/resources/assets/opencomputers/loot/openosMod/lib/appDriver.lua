local thread = require("thread")
local process = require("process")
local event = require("event")
local term = require("term")
local su = require("superUtiles")
local fs = require("filesystem")

----------------------------------------------

local function getCurrentScriptPath()
    local info

    for runLevel = 0, math.huge do
        info = debug.getinfo(runLevel)

        if info then
            if info.what == "main" then
                return info.source:sub(2, -1)
            end
        else
            error("Failed to get debug info for runlevel " .. runLevel)
        end
    end
end

----------------------------------------------

local lib = {}

function lib.create(fullScreen)
    local app = {}
    app.path = getCurrentScriptPath()
    app.folder = fs.path(app.path)
    app.fullScreen = fullScreen
    app.threads = {}
    app.listens = {}
    app.exitFuncs = {}
    if term.isAvailable() then app.isAvailable = true end

    function app.exit()
        for _, func in ipairs(app.exitFuncs) do func() end
        for _, t in ipairs(app.threads) do t:kill() end
        for _, index in ipairs(app.listens) do event.cancel(index) end
        if app.resetGpu then app.resetGpu() end
        if app.isAvailable and app.fullScreen then term.clear() end
        os.exit()
    end

    function app.addThread(t) table.insert(app.threads, t) end
    function app.addListen(l) table.insert(app.listens, l) end

    process.info().data.signal = function() app.exit() end

    if app.isAvailable then
        app.resetGpu = su.saveGpu()
        local gpu = term.gpu()
        gpu.setBackground(0)
        gpu.setForeground(0xFFFFFF)
        if app.fullScreen then
            term.clear()
        end
    end

    return app
end

return lib