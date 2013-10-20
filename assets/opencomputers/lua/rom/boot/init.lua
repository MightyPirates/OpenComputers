local apis = {}
for api in driver.filesystem.dir("lib") do
  local path = "lib/" .. api
  if not driver.filesystem.isDirectory(path) then
    dofile(path)
    table.insert(apis, api)
  end
end
--[[
for _, api in ipairs(apis) do
  if _ENV[api] and type(_ENV[api].install) == "function" then
    _ENV[api].install()
  end
end
apis = nil
]]
component.install()
fs.install()
gpu.install()
term.install()
shell.install()

event.fire(...)
event.wait(math.huge)
