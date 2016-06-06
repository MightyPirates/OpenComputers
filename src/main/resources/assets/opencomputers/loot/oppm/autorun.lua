local fs = require("filesystem")
local shell = require("shell")
fs.mount(...,"/op-manager")
shell.setPath(shell.getPath() .. ":/op-manager")
