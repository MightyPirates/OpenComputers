local component = require("component")
local computer = require("computer")
local event = require("event")

for c, t in component.list() do
  computer.pushSignal("component_added", c, t)
end
os.sleep(0.5) -- Allow signal processing by libraries.
computer.pushSignal("init") -- so libs know components are initialized.

while true do
  require("term").clear()
  io.write(_OSVERSION .. " (" .. math.floor(computer.totalMemory() / 1024) .. "k RAM)\n")
  local result, reason = os.execute(os.getenv("SHELL") .. " -")
  if not result then
    io.stderr:write((tostring(reason) or "unknown error") .. "\n")
    print("Press any key to continue.")
    os.sleep(0.5)
    event.pull("key")
  end
end