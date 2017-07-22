proxy = {}
data = {}
local realroot = computer.getBootAddress()

--------------
-- Utils
local random = math.random
local bootID
local function uuid()
    local template ='xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'
    return string.gsub(template, '[xy]', function (c)
        local v = (c == 'x') and random(0, 0xf) or random(8, 0xb)
        return string.format('%x', v)
    end)
end

--------------
-- Actual code


proxy.address = "procfs000"
proxy.spaceUsed = function() return 0 end
proxy.spaceTotal = function() return 0 end
proxy.makeDirectory = function() error("Permission Denied") end
proxy.isReadOnly = function() return true end
proxy.rename = function() error("Permission Denied") end
proxy.remove = function() error("Permission Denied") end
proxy.setLabel = function() error("Permission Denied") end
proxy.seek = function() error("Not supported") end
proxy.getLabel = function() return "procfs" end

local allocator, handles = kernel.modules.util.getAllocator()

proxy.exists = function()end

proxy.open = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        file = file[d]
    end
    local hnd = allocator:get()
    hnd.data = type(file) == "function" and file() or tostring(file)
    hnd.at = 1
    function hnd:read(n)
        n = n or #self.data
        local d = string.sub(self.data, self.at, self.at + n)
        self.at = self.at + n
        return #d > 0 and d or nil
    end
    return hnd.id
end

proxy.read = function(h, n)
    return handles[h]:read(n)
end

proxy.close = function(h)
    allocator:unset(handles[h])
end

proxy.write = function() error("Permission Denied") end

proxy.size = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local file = data
    for _, d in pairs(seg) do
        file = file[d]
    end
    return type(file) == "function" and #(file()) or 0
end

proxy.isDirectory = function()
    
end

proxy.list = function(path)
    local seg = kernel.modules.vfs.segments(path)
    local dir = data
    for _, d in pairs(seg) do
        dir = dir[d]
    end
    local list = {}
    for pid, thr in pairs(kernel.modules.threading.threads) do
        if thr.coro and dir == data then
            list[#list + 1] = tostring(pid) .. "/"
        end
    end
    if type(dir) ~= "table" then
        return nil, "Not a directory"
    end
    for f, node in pairs(dir) do
        list[#list + 1] = f .. (type(node) == "table" and "/" or "")
    end
    return list
end

data.meminfo = function()
    return "MemTotal: " .. math.floor(computer.totalMemory() / 1024) .. " kB\n"
      .. "MemFree: " .. math.floor(computer.freeMemory() / 1024) .. " kB\n"
      --Buffers??
end

data.cpuinfo = function()
    return "processor   : 0\n" ..
           "vendor_id   : OpenComputersLua\n" ..
           "cpu family  : 1\n" ..
           "model       : 1\n" ..
           "model name  : OpenComputers Lua CPU @ unkown Tier\n" ..
           "microcode   : " .. (string.pack and "0x53" or "0x52") .. "\n" ..
           "physical id : 0\n"
end

data.uptime = function()
    return tostring(computer.uptime())
end

data.sys = {}

data.sys.kernel = {}
data.sys.fs = {}


data.sys.kernel.random = {}

data.sys.kernel.random.uuid = function()
    return uuid()
end

data.sys.kernel.random.boot_id = function()
    return bootID
end

data.sys.kernel.version = function()return _OSVERSION end

data.sys.kernel.poweroff_cmd = function()return "/bin/shutdown.lua" end

data.sys.kernel["real-root-dev"] = function()return realroot end

setmetatable(data, {__index = function(_, k)
    if tonumber(k) and kernel.modules.threading.threads[tonumber(k)] and kernel.modules.threading.threads[tonumber(k)].coro then
        return {
            comm = function()return kernel.modules.threading.threads[tonumber(k)].name end,
            limits = function()
                local limits =     "Limit                     Units    Soft Limit\n"
                limits = limits .. "Max pending signals       signals  " .. kernel.modules.threading.threads[tonumber(k)].maxPendingSignals .. "\n"
                limits = limits .. "Max open files            files    " .. kernel.modules.threading.threads[tonumber(k)].maxOpenFiles .. "\n"
                return  limits
            end,
            status = function()
                local status = ""
                status = status .. "Name: " .. kernel.modules.threading.threads[tonumber(k)].name .. "\n"
                status = status .. "State: " .. coroutine.status(kernel.modules.threading.threads[tonumber(k)].coro) .. "\n"
                status = status .. "Pid: " .. kernel.modules.threading.threads[tonumber(k)].pid .. "\n"
                status = status .. "Uid: " .. kernel.modules.threading.threads[tonumber(k)].uid .. "\n"
                
                --TODO: count actual signals
                status = status .. "SigQ: " .. #kernel.modules.threading.threads[tonumber(k)].eventQueue .. "/" .. kernel.modules.threading.threads[tonumber(k)].maxPendingSignals .. "\n"
                status = status .. "CurH: " .. kernel.modules.threading.threads[tonumber(k)].currentHandler .. "\n"
                return status
            end
        }
    end
end})

function start()
    kernel.modules.vfs.mount(proxy, "/proc")
    bootID = uuid()
    kernel.io.println("Boot UUID is " .. bootID)
end
