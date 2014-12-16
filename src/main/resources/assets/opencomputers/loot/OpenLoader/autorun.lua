local filesystem = require("filesystem")
local shell = require("shell")

local args = {...}
shell.setPath(shell.getPath() .. ":/mnt/"..args[1].address:sub(1,3).."/bin/")

