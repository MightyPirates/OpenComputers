local filesystem = require "filesystem"
local shell = require "shell"
local process = require "process"
print("installNetwork")

local status, installDir = shell.execute("install --noboot --nolabelset --name=Network --fromDir=/data/ --from="..filesystem.get(process.running()).address)

print("You must reboot to start network services")
