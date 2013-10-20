local init = {}
for api in driver.filesystem.dir("lib") do
  local path = "lib/" .. api
  if not driver.filesystem.isDirectory(path) then
    local install = dofile(path)
    if type(install) == "function" then
      table.insert(init, install)
    end
  end
end
for _, install in ipairs(init) do
  install()
end
init = nil

event.fire(...)
event.wait(math.huge)
