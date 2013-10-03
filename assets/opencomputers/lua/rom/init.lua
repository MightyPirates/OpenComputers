dofile("/boot/api/event.lua")
dofile("/boot/api/component.lua")
dofile("/boot/api/filesystem.lua")
dofile("/boot/api/term.lua")
dofile("/boot/sh.lua")

driver.fs.umount("/boot")

event.fire(...)
while true do
  event.fire(os.signal())
end
