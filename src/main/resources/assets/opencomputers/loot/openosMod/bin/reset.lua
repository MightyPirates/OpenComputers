local term = require("term")
local su = require("superUtiles")

local gpu = term.gpu()

---------------------------------------------------

local rx, ry = gpu.maxResolution()
gpu.setResolution(rx, ry)
su.saveFile("/etc/resolution.cfg", "reset")