fs = setmetatable({}, {__index=driver.filesystem})

fs.delete = fs.remove
fs.isFolder = fs.isDirectory
fs.list = fs.dir
fs.mkdir = fs.makeDirectory

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  if component.type(address) == "filesystem" and address ~= os.romAddress() then
    local name = address:sub(1, 3)
    repeat
      name = address:sub(1, name:len() + 1)
    until not fs.exists("/mnt/" .. name)
    fs.mount(address, "/mnt/" .. name)
    local autorun = "/mnt/" .. name .. "/autorun"
    if fs.exists(autorun .. ".lua") then
      dofile(autorun .. ".lua")
    elseif fs.exists(autorun) then
      dofile(autorun)
    end
  end
end

local function onComponentRemoved(_, address)
  if component.type(address) == "filesystem" then
    fs.umount(address)
  end
end

function fs.install()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end

function fs.uninstall()
  event.ignore("component_added", onComponentAdded)
  event.ignore("component_removed", onComponentRemoved)
end
