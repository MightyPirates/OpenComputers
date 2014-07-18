local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)

local mounts = {}
if #args == 0 then
  for proxy, path in fs.mounts() do
    mounts[path] = proxy
  end
else
  for i = 1, #args do
    local proxy, path = fs.get(args[i])
    if not proxy then
      io.stderr:write(args[i], ": no such file or directory\n")
    else
      mounts[path] = proxy
    end
  end
end

local result = {{"Filesystem", "Used", "Available", "Use%", "Mounted on"}}
for path, proxy in pairs(mounts) do
  local label = proxy.getLabel() or proxy.address
  local used, total = proxy.spaceUsed(), proxy.spaceTotal()
  local available, percent
  if total == "unlimited" then
    used = used or "N/A"
    available = "unlimited"
    percent = "0%"
  else
    available = total - used
    percent = used / total
    if percent ~= percent then -- NaN
      available = "N/A"
      percent = "N/A"
    else
      percent = math.ceil(percent * 100) .. "%"
    end
  end
  table.insert(result, {label, used, available, percent, path})
end


local m = {}
for _, entry in ipairs ( result ) do
  for i, e in ipairs (entry) do
    if m[i] == nil then m[i] = 1 end
    m[i] = math.max (m[i],tostring(e):len())
  end
end

for _, entry in ipairs(result) do
  for i,e in ipairs (entry) do
    io.write ( e .. string.rep (' ', (m[i] + 2) - tostring(e):len()) )
  end
  io.write ( '\n' )
end
