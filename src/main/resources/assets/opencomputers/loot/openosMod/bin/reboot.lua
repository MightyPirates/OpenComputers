local computer = require("computer")
local shell = require("shell")

local args, options = shell.parse(...)

io.write("Rebooting...")
computer.shutdown(args[1] or true)