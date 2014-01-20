local event = require("event")

local function onComponentAdded(_, address, componentType)
  if componentType == "filesystem" then
    local proxy = require("component").proxy(address)
    if proxy then
      local fs = require("filesystem")
      local name = address:sub(1, 3)
      while fs.exists(fs.concat("/mnt", name)) and
            name:len() < address:len() -- just to be on the safe side
      do
        name = address:sub(1, name:len() + 1)
      end
      name = fs.concat("/mnt", name)
      fs.mount(proxy, name)
      if isAutorunEnabled then
        local shell = require("shell")
        local result, reason = shell.execute(fs.concat(name, "autorun"), _ENV, proxy)
        if not result then
          error (reason)
        end
      end
    end
  end
end

local function onComponentRemoved(_, address, componentType)
  if componentType == "filesystem" then
    local fs = require("filesystem")
    local shell = require("shell")
    if fs.get(shell.getWorkingDirectory()).address == address then
      shell.setWorkingDirectory("/")
    end
    fs.umount(address)
  end
end

event.listen("component_added", onComponentAdded)
event.listen("component_removed", onComponentRemoved)
