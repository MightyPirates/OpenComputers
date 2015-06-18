--Plan9k userspace init for pipes kernel
local pipes = require("pipes")
local component = require("component")
local filesystem = require("filesystem")

os.setenv("LIBPATH", "/usr/local/lib:/usr/lib:/lib:.")
os.setenv("PATH", "/usr/local/bin:/usr/bin:/bin:.")
os.setenv("PWD", "/")
os.setenv("PS1", "\x1b[33m$PWD\x1b[31m#\x1b[39m ")

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
    
    os.spawnp("/bin/getty.lua", mi, nil, nil, gpu)
    os.spawnp("/bin/readkey.lua", nil, mo, mo, screen, interruptHandler)
    
    if not sout then
        sin = si
        sout = so
        
        io.output(sout)
        io.input(sin)
        
        print("\x1b[32m>>\x1b[39m Starting services")
        local results = require('rc').allRunCommand('start')
        
        for name, result in pairs(results) do
          local ok, reason = table.unpack(result)
          if not ok then
            io.stdout:write("\x1b[31m" .. reason .. "\x1b[39m\n")
          end
        end
    end
    
    io.output(so)
    io.input(si)
    
    print("\x1b[32m>>\x1b[39m Starting Plan9k shell")

    os.spawnp("/bin/sh.lua", si, so, so)
    
end


--local ttyout = io.popen("/bin/getty.lua", "w", ttyconfig)
--local ttyin = io.popen("/bin/readkey.lua", "r", ttyconfig)

local kout = io.popen(function()
    pipes.setThreadName("/bin/tee.lua")
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
