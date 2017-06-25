local component = require("component")
local event = require("event")
local fs = require("filesystem")
local shell = require("shell")

local isInitialized, pendingAutoruns = false, {}

local function onInit()
  isInitialized = true
  for _, run in ipairs(pendingAutoruns) do
    local result, reason = pcall(run)
    if not result then
      local path = fs.concat(os.getenv("TMPDIR") or "/tmp", "event.log")
      local log = io.open(path, "a")
      if log then
        log:write(tostring(result) .. ":" .. tostring(reason) .. "\n")
        log:close()
      end
    end
  end
  pendingAutoruns = nil
end

local function onComponentAdded(_, address, componentType)
  if componentType == "filesystem" and require("computer").tmpAddress() ~= address then
    local proxy = component.proxy(address)
    if proxy then
      local name = address:sub(1, 3)
      while fs.exists(fs.concat("/mnt", name)) and
            name:len() < address:len() -- just to be on the safe side
      do
        name = address:sub(1, name:len() + 1)
      end
      name = fs.concat("/mnt", name)
      fs.mount(proxy, name)
      if fs.isAutorunEnabled() then
        local file = shell.resolve(fs.concat(name, "autorun"), "lua") or
                      shell.resolve(fs.concat(name, ".autorun"), "lua")
        if file then
          local run = function()
            assert(shell.execute(file, _ENV, proxy))
          end
          if isInitialized then
            run()
          else
            table.insert(pendingAutoruns, run)
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

event.listen("init", onInit)
event.listen("component_added", onComponentAdded)
event.listen("component_removed", onComponentRemoved)

require("package").delay(fs, "/lib/core/full_filesystem.lua")
