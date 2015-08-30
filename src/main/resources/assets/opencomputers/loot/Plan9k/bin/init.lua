--Plan9k userspace init for pipes kernel

--TODO: pcall all + emergency shell(or do it higher, in pipes)

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

pipes.log("INIT: Starting terminals")

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

local screens = component.list("screen")
for gpu in component.list("gpu") do
    local screen = screens()
    if not screen then break end
    component.invoke(gpu, "bind", screen)
    
    local pty, mi, mo, si, so = pipes.openPty()
    
    local interruptHandler = function()
        print("SIGINT!!")
    end
    
    os.spawnp("/bin/getty.lua", mi, mo, nil, gpu)
    os.spawnp("/bin/readkey.lua", nil, mo, mo, screen, interruptHandler)
    
    if not sout then
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

    os.spawnp("/bin/sh.lua", si, so, so)
    
end

if not sout then
    sout = io.open("/dev/null", "w")
    io.output(sout)
end

pcall(services)

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

for address, ctype in component.list() do
    computer.pushSignal("component_added", address, ctype)
end

computer.pushSignal("init")
os.sleep(0)

while true do
    local sig = {computer.pullSignal()}
    if sig[1] == "component_added" then
        if sig[3] == "filesystem" then
            local proxy = component.proxy(sig[2])
            if proxy then
                local name = sig[2]:sub(1, 3)
                while filesystem.exists(filesystem.concat("/mnt", name)) and name:len() < sig[2]:len() do
                    name = sig[2]:sub(1, name:len() + 1)
                end
                name = filesystem.concat("/mnt", name)
                filesystem.mount(proxy, name)
            end
        end
    elseif sig[1] == "component_removed" then
        if sig[3] == "filesystem" then
            filesystem.umount(sig[2])
        end
    end
end
