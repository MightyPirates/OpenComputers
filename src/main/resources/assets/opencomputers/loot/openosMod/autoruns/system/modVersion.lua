local event = require("event")
local su = require("superUtiles")
local component = require("component")
local serialization = require("serialization")

--------------------------------------------------------------

local inData = assert(serialization.unserialize(assert(su.getFile("/version.cfg"))))

--------------------------------------------------------------

_G._MODVERSION = inData.version
_G._OSVERSION = _G._OSVERSION .. " | modVersion " .. tostring(_G._MODVERSION)