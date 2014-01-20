local component = require("component")
local computer = require("computer")

for c, t in component.list() do
  computer.pushSignal("component_added", c, t)
end
os.sleep(0.5) -- Allow signal processing by libraries.

require("term").clear()

while true do
  local result, reason = os.execute("/bin/sh -v")
  if not result then
    print(reason)
  end
end