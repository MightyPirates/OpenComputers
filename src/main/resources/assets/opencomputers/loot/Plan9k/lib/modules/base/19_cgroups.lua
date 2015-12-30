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
        
    group.preload = {
        package = kernel.userspace.package, --TODO TODO TODO: METATABLE THIZ!!!!!!!!
        filesystem = setmetatable({}, {__index = kernel.modules.vfs}),
        buffer = setmetatable({}, {__index = kernel.modules.buffer}),
        bit32 = setmetatable({}, {__index = kernel.userspace.bit32}),
        component = setmetatable({}, {__index = kernel.userspace.component}),
        computer = setmetatable({}, {__index = kernel.userspace.computer}),
        io = setmetatable({}, {__index = kernel.modules.io.io}),
        unicode = setmetatable({}, {__index = kernel.userspace.unicode}),
    }
    group.loaded = {}
    group.loading = {}
    group.searchers = {}
    return group
end

groupConstructors.component = function(parent, wl, bl)
    local group = newGroup()
    group.parent = parent
    group.whitelist = wl
    group.blacklist = bl
    group.adding = not parent and kernel.modules.component.kernelGroup.adding or {}
    group.removing = not parent and kernel.modules.component.kernelGroup.removing or {}
    group.primaries = not parent and kernel.modules.component.kernelGroup.primaries or {}
    group.allow = function(addr)
        if not group.parent or group.parent.allow(addr) then
            return (not group.whitelist or group.whitelist[addr]) and (not group.blacklist or (not group.blacklist[addr]))
        end
    end
    return group
end

------------------------------

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

spawnGroupGetters.component = function()
    if not kernel.modules.threading.currentThread then
        return groupConstructors.component()
    else
        return kernel.modules.threading.currentThread.cgroups.component
    end
end

------------------------------
--Signal group functions

function pushSignal(...)
    for k, thread in pairs(kernel.modules.threading.currentThread.cgroups.signal.processes) do
        thread.eventQueue[#thread.eventQueue + 1] = {"signal", ...}
    end
end

------------------------------

userConstructors = {}

function userConstructors.module()
    return groupConstructors.module()
end

function userConstructors.component(wl, bl)
    return groupConstructors.component(kernel.modules.threading.currentThread and kernel.modules.threading.currentThread.cgroups.component, wl, bl)
end

------------------------------

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

