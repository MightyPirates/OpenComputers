local networks = require("networks")
local event = require("event")

local bigchat = networks.create(networks.getDevices(true, true), "bigchat", true)

--------------------------------------------------

local function listen(_, net, ...)
    if net == bigchat.name then
        event.push("big_chat", ...)
    end
end
event.listen("network_message", listen)

--------------------------------------------------

local lib = {}

lib.send = function(...)
    bigchat.send(...)
end

return lib