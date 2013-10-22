local isAutorunEnabled = true

-------------------------------------------------------------------------------

filesystem = setmetatable({}, {__index=driver.filesystem})
fs = filesystem

fs.delete = fs.remove
fs.isFolder = fs.isDirectory
fs.list = fs.dir
fs.mkdir = fs.makeDirectory

-------------------------------------------------------------------------------

function fs.autorun(...)
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(1, args[1], "boolean")
    isAutorunEnabled = args[1]
  end
  return isAutorunEnabled
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  local componentType = component.type(address)
  if (componentType == "filesystem" or componentType == "disk_drive") and
     address ~= os.romAddress() and
     address ~= os.tmpAddress()
  then
    local name = address:usub(1, 3)
    repeat
      name = address:usub(1, name:ulen() + 1)
    until not fs.exists("/mnt/" .. name)
    fs.mount(address, "/mnt/" .. name)
    if isAutorunEnabled then
      shell.execute("/mnt/" .. name .. "/autorun")
    end
  end
end

local function onComponentRemoved(_, address, componentType)
  if componentType == "filesystem" or componentType == "disk_drive" then
    while fs.umount(address) do end -- remove *all* mounts
  end
end

return function()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end
