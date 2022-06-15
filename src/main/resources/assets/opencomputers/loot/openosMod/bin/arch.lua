local arch = require("archlib2")
local su = require("superUtiles")
local shell = require("shell")

local args, options = shell.parse(...)

------------------------------------

if args[1] == "pack" then
    local dir = shell.resolve(args[2])
    local file = shell.resolve(args[3])

    arch.packPro(dir, file, not options.a)
elseif args[1] == "unpack" then
    local file = shell.resolve(args[2])
    local dir = shell.resolve(args[3])

    arch.unpackPro(dir, file, not options.a)
else
    print("arch pack directory outputfile [-a(not not compress)]")
    print("arch unpack inputfile outputdirectory [-a if arch is not compressed]")
end