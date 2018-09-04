local shell = require("shell")
return loadfile(shell.resolve("less", "lua"))("--noback", ...)
