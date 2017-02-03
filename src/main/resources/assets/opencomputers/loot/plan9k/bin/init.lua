--Plan9k userspace init for pipes kernel

--TODO: pcall all + emergency shell(or do it lower, in pipes)

local pipes = require("pipes")
local filesystem = require("filesystem")
local component = require("component")

os.setenv("LIBPATH", "/lib/?.lua;/usr/lib/?.lua;/home/lib/?.lua;./?.lua;/lib/?/init.lua;/usr/lib/?/init.lua;/home/lib/?/init.lua;./?/init.lua")
os.setenv("PATH", "/usr/local/bin:/usr/bin:/bin:.")
os.setenv("PWD", "/")
os.setenv("PS1", "\x1b[33m$PWD\x1b[31m#\x1b[39m ")

pipes.log("INIT: Mounting filesystems")

if filesystem.exists("/etc/fstab") then
    for entry in io.lines("/etc/fstab") do
        if entry:sub(1,1) ~= "#" then
            
        end
    end
end

pipes.log("INIT: Starting Plan9k")

if not filesystem.exists("/root") then
    filesystem.makeDirectory("/root")
end
os.setenv("HOME", "/root")
os.setenv("PWD", "/root")

local hostname = io.open("/etc/hostname")
if hostname then
    local name = hostname:read("*l")
    hostname:close()
    os.setenv("HOSTNAME", name)
    os.setenv("PS1", "\x1b[33m$HOSTNAME\x1b[32m:\x1b[33m$PWD\x1b[31m#\x1b[39m ")
    computer.pushSignal("hostname", name)
end
local services
services = function()
    local results = require('rc').allRunCommand('start')
    
    for name, result in pairs(results) do
      local ok, reason = table.unpack(result)
      if not ok then
        io.stdout:write("\x1b[31m" .. reason .. "\x1b[39m\n")
      end
    end
    services = function()end
end

local sin, sout

local services_ran = false

local free_gpus = {}
local free_screens = {}
local used_gpus = {}
local used_screens = {}

local function runtty(gpu, screen)
    component.invoke(gpu, "bind", screen)
    
    local pty, mi, mo, si, so = pipes.openPty()
    
    local interruptHandler = function()
        print("SIGINT!!")
    end
    
    local getty = os.spawnp("/bin/getty.lua", mi, mo, nil, gpu, screen)
    local readkey = os.spawnp("/bin/readkey.lua", nil, mo, mo, screen, interruptHandler)
    
    if not services_ran then
        services_ran = true
        
        sin = si
        sout = so
        
        io.output(sout)
        io.input(sin)
        
        print("\x1b[32m>>\x1b[39m Starting services")
        pcall(services)
    end
    
    io.output(so)
    io.input(si)
    
    if debug.kexec then
        print("\x1b[32m>>\x1b[31m KERNEL DEBUG MODULE IS LOADED\x1b39m")
    end
    
    print("\x1b[32m>>\x1b[39m Starting Plan9k shell")

    local sh = os.spawnp("/bin/sh.lua", si, so, so)
    
    return {gpu = gpu, screen = screen, childs = {getty, readkey, sh}}
end

local function stoptty(ttyinfo)
    for _, pid in ipairs(ttyinfo.childs) do
        pcall(os.kill, pid, "kill")
    end
end

--[[local screens = component.list("screen")
for gpu in component.list("gpu") do
    local screen = screens()
    if not screen then break end
    
    runtty(gpu, screen)
end]]--

if not sout then
    sout = io.open("/dev/null", "w")
    io.output(sout)
end

--pcall(services)

local kout = io.popen(function()
    if filesystem.exists("/kern.log") then
        filesystem.remove("/kern.log.old")
        filesystem.rename("/kern.log", "/kern.log.old")
    end
    pipes.setThreadName("[init]/logd")
    io.output(sout)
    loadfile("/bin/tee.lua", nil, _G)("/kern.log")
end, "w")

pipes.setKernelOutput(kout)

--computer.pullSignal()

if not filesystem.isDirectory("/mnt") then
    filesystem.makeDirectory("/mnt")
end

if not filesystem.isDirectory("/etc") then
    filesystem.makeDirectory("/etc")
end


for address, ctype in component.list() do
    computer.pushSignal("component_added", address, ctype)
end

computer.pushSignal("init")

local signal = {}
local on_component_add = {}
local on_component_remove = {}

----
-- Filesystem

function on_component_add.filesystem(addr)
    local proxy = component.proxy(addr)
    if proxy then
        local name = addr:sub(1, 3)
        while filesystem.exists(filesystem.concat("/mnt", name)) and name:len() < addr:len() do
            name = addr:sub(1, name:len() + 1)
        end
        name = filesystem.concat("/mnt", name)
        filesystem.mount(proxy, name)
    end
end

function on_component_remove.filesystem(addr)
    filesystem.umount(addr)
end

----
-- TTY

function on_component_add.gpu(gpu)
    if used_gpus[gpu] then return end
    if #free_screens > 0 then
        local screen = table.remove(free_screens, 1)
        local ttyinfo = runtty(gpu, screen)
        used_screens[screen] = ttyinfo
        used_gpus[gpu] = ttyinfo
    else
        free_gpus[#free_gpus + 1] = gpu
    end
end

function on_component_add.screen(screen)
    if used_screens[screen] then return end
    if #free_gpus > 0 then
        local gpu = table.remove(free_gpus, 1)
        local ttyinfo = runtty(gpu, screen)
        used_screens[screen] = ttyinfo
        used_gpus[gpu] = ttyinfo
    else
        free_screens[#free_screens + 1] = screen
    end
end

function on_component_remove.gpu(gpu)
    if used_gpus[gpu] then
        local ttyinfo = used_gpus[gpu]
        used_gpus[gpu] = nil
        stoptty(ttyinfo)
        used_screens[ttyinfo.screen] = nil
        free_screens[#free_screens + 1] = ttyinfo.screen
    else
        for k, v in ipairs(free_gpus) do
            if v == gpu then
                table.remove(free_gpus, k)
                break
            end
        end
    end
end

function on_component_remove.screen(screen)
    if used_screens[screen] then
        local ttyinfo = used_screens[screen]
        used_screens[screen] = nil
        stoptty(ttyinfo)
        used_gpus[ttyinfo.gpu] = nil
        free_gpus[#free_gpus + 1] = ttyinfo.gpu
    else
        for k, v in ipairs(free_screens) do
            if v == screen then
                table.remove(free_screens, k)
                break
            end
        end
    end
end

----

function signal.component_added(_, addr, ctype, ...)
    if on_component_add[ctype] then
        on_component_add[ctype](addr, ctype, ...)
    end
end


function signal.component_removed(_, addr, ctype, ...)
    if on_component_remove[ctype] then
        on_component_remove[ctype](addr, ctype, ...)
    end
end

function signal.init()
    if not services_ran then
        services_ran = true
        
        pcall(services)
    end
end

os.sleep(0)

while true do
    local sig = {computer.pullSignal()}
    if signal[sig[1]] then
        signal[sig[1]](table.unpack(sig))
    end
end
