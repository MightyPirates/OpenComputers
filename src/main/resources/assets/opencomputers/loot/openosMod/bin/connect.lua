local networks = require("networks")
local shell = require("shell")
local distfs2 = require("distfs2")

----------------------------------------------

local args, options = shell.parse(...)

----------------------------------------------

if args[1] == "add" then
    local network = networks.getNetwork(args[2])
    if not network then io.stderr:write("this network in not open\n") return end
    distfs2.connect(network, args[3], shell.resolve(args[4]))
else
    print("usage:")
    print("add network index folder")
end