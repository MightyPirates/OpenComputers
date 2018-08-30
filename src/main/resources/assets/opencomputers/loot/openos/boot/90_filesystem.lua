local event = require("event")
local fs = require("filesystem")
local shell = require("shell")
local tmp = require("computer").tmpAddress()

local pendingAutoruns = {}

local function onComponentAdded(_, address, componentType)
  if componentType == "filesystem" and tmp ~= address then
    local proxy = fs.proxy(address)
    if proxy then
      local name = address:sub(1, 3)
      while fs.exists(fs.concat("/mnt", name)) and
            name:len() < address:len() -- just to be on the safe side
      do
        name = address:sub(1, name:len() + 1)
      end
      name = fs.concat("/mnt", name)
      fs.mount(proxy, name)
      if not fs.exists("/etc/filesystem.cfg") or fs.isAutorunEnabled() then
        local file = shell.resolve(fs.concat(name, "autorun"), "lua") or
                      shell.resolve(fs.concat(name, ".autorun"), "lua")
        if file then
          local run = {file, _ENV, proxy}
          if pendingAutoruns then
            table.insert(pendingAutoruns, run)
          else
            xpcall(shell.execute, event.onError, table.unpack(run))
          end
        end
      end
    end
  end
end

local function onComponentRemoved(_, address, componentType)
  if componentType == "filesystem" then
    if fs.get(shell.getWorkingDirectory()).address == address then
      shell.setWorkingDirectory("/")
    end
    fs.umount(address)
  end
end

event.listen("init", function()
  for _, run in ipairs(pendingAutoruns) do
    xpcall(shell.execute, event.onError, table.unpack(run))
  end
  pendingAutoruns = nil
  return false
end)

event.listen("component_added", onComponentAdded)
event.listen("component_removed", onComponentRemoved)

require("package").delay(fs, "/lib/core/full_filesystem.lua")
