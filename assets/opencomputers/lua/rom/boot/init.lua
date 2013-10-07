for _, api in pairs{driver.filesystem.dir("lib")} do
  local path = "lib/" .. api
  if not driver.filesystem.isDirectory(path) then
    dofile(path)
  end
end

component.install()
fs.install()
term.install()

dofile("/bin/sh.lua")

event.fire(...)
while true do
  coroutine.sleep()
end
