local event = require "event"
local network = require "network"
local component = require "component"

network.tcp.listen(22)

while true do
    local evt = {event.pullFiltered(function(ev, action, ch, remote, port, inc)
        if ev ~= "tcp" or action ~= "connection" or port ~= 22 or inc ~= "incoming" then
            return false
        end
        return true
    end)}
    os.spawnp("/usr/sbin/sshsession.lua", nil, nil, nil, evt[3])
    --os.spawn("/usr/sbin/sshsession.lua", evt[3])
end
