local pipes = require("pipes")
local term = require("term")
local args = {...}

while true do
    term.clear()
    local pid = os.spawn(...)
    pipes.joinThread(pid)
    os.sleep(1)
end
