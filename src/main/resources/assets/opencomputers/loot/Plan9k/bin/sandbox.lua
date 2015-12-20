local shell = require("shell")
local kernel = require("pipes")

local args, options = shell.parse(...)

if options.h or options.help then
    print([[sandbox {-m,-s} [file]
    Sandbox a process
        -m, --module - sandbox modules
        -s, --signal - sandbox signals
        ]])
    return
end


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
