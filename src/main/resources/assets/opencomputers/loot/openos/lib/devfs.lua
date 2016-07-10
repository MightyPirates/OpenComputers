local fs = require("filesystem")

local devfs = {points={},address=require("uuid").next()}

local bfd = "bad file descriptor"

function devfs.getLabel()
  return "devfs"
end

function devfs.setLabel(value)
  error("drive does not support labeling")
end

function devfs.spaceTotal()
  return 0
end

function devfs.spaceUsed()
  return 0
end

function devfs.exists(path)
  return not not devfs.points[path]
end

function devfs.size(path)
  return 0
end

function devfs.isDirectory(path)
  return false
end

function devfs.lastModified(path)
  return fs.lastModified("/dev/")
end

function devfs.list()
  local keys = {}
  for k,v in pairs(devfs.points) do
    table.insert(keys, k)
  end
  return keys
end

function devfs.makeDirectory(path)
  return false
end

function devfs.remove(path)
  if not devfs.exists(path) then return false end
  devfs.points[path] = nil
  return true
end

function devfs.rename(from, to)
  return false
end

function devfs.open(path, mode)
  checkArg(1, path, "string")

  local handle = devfs.points[path]
  if not handle then return nil, "device point [" .. path .. "] does not exist" end

  if handle.open then
    return handle:open(path, mode)
  end

  local msg = "device point [" .. path .. "] cannot be opened for "

  if mode == "r" then
    if not handle.read then
      return nil, msg .. "read"
    end
  else
    if not handle.write then
      return nil, msg .. "write"
    end
  end

  return handle
end

function devfs.read(h,...)
  if not h.read then return nil, bfd end
  return h:read(...)
end

function devfs.seek(h,...)
  if not h.seek then return nil, bfd end
  return h:seek(...)
end

function devfs.write(h,...)
  if not h.write then return nil, bfd end
  return h:write(...)
end

function devfs.close(h, ...)
  if not h.close then return nil, bfd end
  return h:close(...)
end

function devfs.create(path, handle)
  devfs.points[path] = handle
  return true
end

devfs.create("null", {write = function()end})
devfs.create("random", {read = function(_,n)
  local chars = {}
  for i=1,n do
    table.insert(chars,string.char(math.random(0,255)))
  end
  return table.concat(chars)
end})

return devfs
