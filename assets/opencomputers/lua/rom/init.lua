dofile("/rom/api/event.lua")
dofile("/rom/api/component.lua")
dofile("/rom/api/term.lua")
dofile("/rom/sh.lua")

event.fire(...)
while true do
  event.fire(os.signal())
end
