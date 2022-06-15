local computer = require("computer")
local shell = require("shell")
local text = require("text")

local args, options = shell.parse(...)

local devices = computer.getDeviceInfo()
local columns = {}

if not next(options, nil) then
  options.t = true
  options.d = true
  options.p = true
end
if options.t then table.insert(columns, "Class") end
if options.d then table.insert(columns, "Description") end
if options.p then table.insert(columns, "Product") end
if options.v then table.insert(columns, "Vendor") end
if options.c then table.insert(columns, "Capacity") end
if options.w then table.insert(columns, "Width") end
if options.s then table.insert(columns, "Clock") end

local m = {}
for address, info in pairs(devices) do
  for col, name in ipairs(columns) do
    m[col] = math.max(m[col] or 1, (info[name:lower()] or ""):len())
  end
end

io.write(text.padRight("Address", 10))
for col, name in ipairs(columns) do
  io.write(text.padRight(name, m[col] + 2))
end
io.write("\n")

for address, info in pairs(devices) do
  io.write(text.padRight(address:sub(1, 5).."...", 10))
  for col, name in ipairs(columns) do
    io.write(text.padRight(info[name:lower()] or "", m[col] + 2))
  end
  io.write("\n")
end
