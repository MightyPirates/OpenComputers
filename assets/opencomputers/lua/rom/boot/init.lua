for _, api in pairs{driver.filesystem.dir("lib")} do
  local path = "lib/" .. api
  if not driver.filesystem.isDirectory(path) then
    dofile(path)
  end
end

component.install()
fs.install()
gpu.install()
term.install()

dofile("/bin/sh.lua")

event.fire(...)
event.wait(math.huge)
