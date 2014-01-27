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
      local log = io.open("/tmp/event.log", "a")
      if log then
        log:write(reason .. "\n")
        log:close()
      end
    end
  end
  pendingAutoruns = nil
end

local function onComponentAdded(_, address, componentType)
  if componentType == "filesystem" then
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
        local function run()
          local result, reason = shell.execute(fs.concat(name, "autorun"), _ENV, proxy)
          if not result and reason ~= "file not found" then
            error(reason, 0)
          end
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
