local fs = require("filesystem")
local shell = require("shell")
local text = require("text")

local args, options = shell.parse(...)

local function formatSize(size)
  if not options.h then
    return tostring(size)
  elseif type(size) == "string" then
    return size
  end
  local sizes = {"", "K", "M", "G"}
  local unit = 1
  local power = options.si and 1000 or 1024
  while size > power and unit < #sizes do
    unit = unit + 1
    size = size / power
  end
  return math.floor(size * 10) / 10 .. sizes[unit]
end

local mounts = {}
if #args == 0 then
  for proxy, path in fs.mounts() do
    if not mounts[proxy] or mounts[proxy]:len() > path:len() then
      mounts[proxy] = path
    end
  end
else
  for i = 1, #args do
    local proxy, path = fs.get(shell.resolve(args[i]))
    if not proxy then
      io.stderr:write(args[i], ": no such file or directory\n")
    else
      mounts[proxy] = path
    end
  end
end

local result = {{"Filesystem", "Used", "Available", "Use%", "Mounted on"}}
for proxy, path in pairs(mounts) do
  local label = proxy.getLabel() or proxy.address
  local used, total = proxy.spaceUsed(), proxy.spaceTotal()
  local available, percent
  if total == math.huge then
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
  table.insert(result, {label, formatSize(used), formatSize(available), tostring(percent), path})
end

local m = {}
for _, row in ipairs(result) do
  for col, value in ipairs(row) do
    m[col] = math.max(m[col] or 1, value:len())
  end
end

for _, row in ipairs(result) do
  for col, value in ipairs(row) do
    local padding = col == #row and 0 or 2
    io.write(text.padRight(value, m[col] + padding))
  end
  print()
end
