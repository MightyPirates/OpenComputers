-- Aliases for vanilla Lua.
os.remove = driver.fs.remove
os.rename = driver.fs.rename
-- TODO os.tmpname = function() end

io = {}
-- TODO io.flush = function() end
-- TODO io.lines = function(filename) end
io.open = driver.fs.open
-- TODO io.popen = function(prog, mode) end
io.read = driver.fs.read
-- TODO io.tmpfile = function() end
io.type = driver.fs.type

-------------------------------------------------------------------------------

event.listen("component_added", function(_, address)
  if component.type(address) == "filesystem" and address ~= os.romAddress() then
    local name = address:sub(1, 3)
    repeat
      name = address:sub(1, name:len() + 1)
    until not driver.fs.exists("/mnt/" .. name)
    driver.fs.mount(address, "/mnt/" .. name)
    local autorun = "/mnt/" .. name .. "/autorun"
    if driver.fs.exists(autorun .. ".lua") then
      dofile(autorun .. ".lua")
    elseif driver.fs.exists(autorun) then
      dofile(autorun)
    end
  end
end)

event.listen("component_removed", function(_, address)
  if component.type(address) == "filesystem" then
    driver.fs.umount(address)
  end
end)
