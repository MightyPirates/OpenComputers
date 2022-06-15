local networks = require("networks")
local shell = require("shell")
local distfs2 = require("distfs2")

----------------------------------------------

local args, options = shell.parse(...)

----------------------------------------------

if args[1] == "list" then
    local hosts = distfs2.hosts
    for i = 1, #hosts do
        print(hosts[i].index)
    end
elseif args[1] == "kill" then
    local host
    local hosts = distfs2.hosts
    for i = 1, #hosts do
        if hosts[i].index == args[2] then
            host = hosts[i]
            break
        end
    end
    if not host then io.stderr:write("this host in not open\n") return end
    host.kill()
elseif args[1] == "add" then
    local network = networks.getNetwork(args[2])
    if not network then io.stderr:write("this network in not open\n") return end
    distfs2.create(network, args[3], shell.resolve(args[4]), args[5] == "true")
else
    print("usage:")
    print("list")
    print("kill index")
    print("add network index folder readonly")
end