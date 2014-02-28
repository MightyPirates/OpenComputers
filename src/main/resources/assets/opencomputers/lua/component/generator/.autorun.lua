local event = require("event")
local fs = require("filesystem")
local process = require("process")

local proxy = ...

-- Install symlinks if they don't already exist.
local links = {}
local fsroot = fs.path(process.running())
local function inject(path)
  for file in fs.list(fs.concat(fsroot, path)) do
    local source = fs.concat(fsroot, path, file)
    local target = fs.concat(path, file)
    if fs.link(source, target) then
      table.insert(links, target)
    end
  end
end
inject("lib")
inject("bin")
inject("usr/man")

-- Delete symlinks on removal.
event.listen("component_removed", function(_, address)
  if address == proxy.address then
    for _, link in ipairs(links) do
      fs.remove(link)
    end
    return false -- remove listener
  end
end)