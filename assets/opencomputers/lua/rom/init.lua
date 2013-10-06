dofile("/boot/api/event.lua")
dofile("/boot/api/component.lua")
dofile("/boot/api/term.lua")
dofile("/boot/bin/automount.lua")
dofile("/boot/bin/sh.lua")

driver.fs.umount("/boot")

event.fire(...)
while true do
  coroutine.sleep()
end
