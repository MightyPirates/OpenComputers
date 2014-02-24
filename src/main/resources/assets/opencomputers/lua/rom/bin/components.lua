local component = require("component")

for address, name in component.list() do
  io.write(name .. "\t" .. address .. "\n")
end