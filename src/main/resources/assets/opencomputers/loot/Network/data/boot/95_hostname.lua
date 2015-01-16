local network = require "network"
local event = require "event"

event.listen("network_ready", function()
    pcall(function()
        for name in io.lines("/etc/hostname")do
            network.ip.bind(name)
        end
    end)
end)

