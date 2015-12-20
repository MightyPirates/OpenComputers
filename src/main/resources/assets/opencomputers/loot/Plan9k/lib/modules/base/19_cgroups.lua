local function newGroup()
    local group = {
        childs = {},
        processes = {}
    }
    return group
end

spawnGroupGetters = {}
groupConstructors = {}

groupConstructors.signal = function(global)
    local group = newGroup()
    group.global = global --boolean
    return group
end

groupConstructors.filesystem = function(root)
    local group = newGroup()
    group.root = root
    return group
end

groupConstructors.network = function()
    local group = newGroup()
    group.interfaces = {}
    return group
end

groupConstructors.module = function()
    local group = newGroup()
    group.preload = {}
    group.loaded = {}
    group.loading = {}
    group.searchers = {}
    return group
end

spawnGroupGetters.signal = function()
    if not kernel.modules.threading.currentThread then
        return groupConstructors.signal(true)
    else
        return kernel.modules.threading.currentThread.cgroups.signal
    end
end

spawnGroupGetters.filesystem = function()
    if not kernel.modules.threading.currentThread then
        return groupConstructors.filesystem(true)
    else
        return kernel.modules.threading.currentThread.cgroups.filesystem
    end
end

spawnGroupGetters.network = function()
    if not kernel.modules.threading.currentThread then
        return groupConstructors.network()
    else
        return kernel.modules.threading.currentThread.cgroups.network
    end
end

spawnGroupGetters.module = function()
    if not kernel.modules.threading.currentThread then
        return groupConstructors.module()
    else
        return kernel.modules.threading.currentThread.cgroups.module
    end
end

--Signal group functions

function pushSignal(...)
    for k, thread in pairs(kernel.modules.threading.currentThread.cgroups.signal.processes) do
        thread.eventQueue[#thread.eventQueue + 1] = {"signal", ...}
    end
end

userConstructors = {}

function userConstructors.module()
    return groupConstructors.module()
end

function new(pid, name, ...)
    if kernel.modules.threading.currentThread.pid ~= pid and 
        (not kernel.modules.threading.threads[pid] or
         not kernel.modules.threading.threads[pid].parent or
         kernel.modules.threading.threads[pid].parent.uid ~= kernel.modules.threading.currentThread.uid) then
        error("Permission denied")
    end
    if not userConstructors[name] then
        error("No cgroup constructor for name")
    end
    kernel.modules.threading.threads[pid].cgroups[name] = userConstructors[name](...)
end

