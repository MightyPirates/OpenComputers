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

-- TODO tabulate
for _, entry in ipairs(result) do
  io.write(table.concat(entry, "\t"), "\n")
end