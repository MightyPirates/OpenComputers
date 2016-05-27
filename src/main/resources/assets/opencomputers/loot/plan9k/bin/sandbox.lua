local shell = require("shell")
local kernel = require("pipes")
local component = require("component")

local args = {...}
local options = {}

for _, v in ipairs(args) do
    if v:sub(1,2) == "--" then
        options[v:sub(3)] = true
    elseif v:sub(1,1) == "-" then
        options[v:sub(2)] = true
    end
end

if options.h or options.help then
    print([[sandbox [flow] [spawn [file] [args Nargs ...] ]...
    Sandbox a processes/process tree
        Options:
          -h --help - this help
        Commands:
          spawn file [args Nargs ...] - spawn process
          join - wait for last process to finish
          module - create module cgroup
          component - create component cgroup
            befor this command put:
            wl [addr] - whitelist a component
            bl [addr] - blacklist a component
          quietin, quietout, quieterr - disable stdin/out/err
    Example usages:
      sandbox module spawn /usr/bin/myprog.lua args 3 hello 123 world join
        - Spawns myprog in new module cgroup with 3 arguments and wait for it
      sandbox module spawn /bin/a.lua spawn /bin/b.lua
        - Spawns 2 programs in new common module cgroup
      sandbox bl 0ab component spawn /bin/a.lua
        - Spawn process with disallowed access to componet starting with address 0ab
      sandbox wl fc0 wl fcd component spawn /bin/a.lua
        - Spawns process wich can only acces 2 specified components
      sandbox -b quietin quietout /bin/a.lua
        - Spawns process in background 
        ]])
    return
end

--[[
local program = os.getenv("SHELL") or "/bin/sh.lua"

if #args > 0 then
    program = table.remove(args, 1)
end

local module
if options.m or options.module then
    module = kernel.setns(kernel.getPid(), "module")
end

local pid = os.spawn(program, table.unpack(args))

kernel.joinThread(pid)
]]

local lastpid = nil
local stdin = io.stdin
local stdout = io.stdout
local stderr = io.stderr

local module
local componentcg
local wl = nil
local bl = nil
local n =1 
while n <= #args do
    if args[n]:sub(1,1) == "-" then
    elseif args[n] == "spawn" then
        local name = args[n + 1]
        local arg = {}
        if args[n+2] == "args" then
            local narg = tonumber(args[n + 3])
            for i = 1, narg do
                arg[#arg + 1]= args[n + 3 + i]
            end
            n = n + 2 + narg
        end
        n = n + 1
        lastpid = os.spawnp(name, stdin, stdout, stderr, table.unpack(arg))
    elseif args[n] == "join" then
        kernel.joinThread(lastpid)
    elseif args[n] == "quietin" then
        stdin = io.open("/dev/null", "r")
    elseif args[n] == "quietout" then
        stdout = io.open("/dev/null", "w")
    elseif args[n] == "quieterr" then
        stderr = io.open("/dev/null", "w")
    elseif args[n] == "module" then
        module = kernel.setns(kernel.getPid(), "module")
    elseif args[n] == "wl" then
        if not wl then wl = {} end
        wl[component.get(args[n + 1])] = true
        n = n + 1
    elseif args[n] == "bl" then
        if not bl then bl = {} end
        bl[component.get(args[n + 1])] = true
        n = n + 1
    elseif args[n] == "component" then
        componentcg = kernel.setns(kernel.getPid(), "component", wl, bl)
    else
        print("Unknown command '"..args[n] .."'")
        return
    end
    n = n + 1
end
