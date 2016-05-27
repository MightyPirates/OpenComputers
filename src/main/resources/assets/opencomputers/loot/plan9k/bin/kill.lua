local args = {...}
local pid = tonumber(args[1])
if not pid then error("Usage: kill [pid]") end
local res, reason = os.kill(pid, "kill")

if not res then
    error(reason)
end
