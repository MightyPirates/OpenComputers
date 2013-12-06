fs.mount(computer.romAddress(), "/")
if computer.tmpAddress() then fs.mount(computer.tmpAddress(), "/tmp") end

for c, t in component.list() do
  computer.pushSignal("component_added", c, t)
end
os.sleep(0.5) -- Allow signal processing by libraries.

term.clear()

while true do
  local result, reason = os.execute("/bin/sh -v")
  if not result then
    print(reason)
  end
end