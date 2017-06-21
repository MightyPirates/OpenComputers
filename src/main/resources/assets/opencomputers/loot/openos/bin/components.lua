local component = require("component")
local shell = require("shell")
local text = require("text")

local args, options = shell.parse(...)
local count = tonumber(options.limit) or math.huge

local components = {}
local padTo = 1

if #args == 0 then -- get all components if no filters given.
  args[1] = ""
end
for _, filter in ipairs(args) do
  for address, name in component.list(filter) do
    if name:len() > padTo then
      padTo = name:len() + 2
    end
    components[address] = name
  end
end

padTo = padTo + 8 - padTo % 8
for address, name in pairs(components) do
  io.write(text.padRight(name, padTo) .. address .. '\n')

  if options.l then
    local proxy = component.proxy(address)
    local padTo = 1
    local methods = {}
    for name, member in pairs(proxy) do
      if type(member) == "table" or type(member) == "function" then
        if name:len() > padTo then
          padTo = name:len() + 2
        end
        table.insert(methods, name)
      end
    end
    table.sort(methods)
    padTo = padTo + 8 - padTo % 8

    for _, name in ipairs(methods) do
      local doc = component.doc(address, name) or tostring(proxy[name])
      io.write("  " .. text.padRight(name, padTo) .. doc .. '\n')
    end
  end

  count = count - 1
  if count <= 0 then
    break
  end
end
