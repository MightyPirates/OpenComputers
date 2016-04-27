local fs = require("filesystem")

local proxy = {points={},address=require("guid").next()}

local nop = function()end

function proxy.getLabel()
  return "devfs"
end

function proxy.setLabel(value)
  error("drive does not support labeling")
end

function proxy.isReadOnly()
  return false
end

function proxy.spaceTotal()
  return 0
end

function proxy.spaceUsed()
  return 0
end

function proxy.exists(path)
  return not not proxy.points[path]
end

function proxy.size(path)
  return 0
end

function proxy.isDirectory(path)
  return false
end

function proxy.lastModified(path)
  return fs.lastModified("/dev/")
end

function proxy.list()
  local keys = {}
  for k,v in pairs(proxy.points) do
    table.insert(keys, k)
  end
  return keys
end

function proxy.makeDirectory(path)
  return false
end

function proxy.remove(path)
  if not proxy.exists(path) then return false end
  proxy.points[path] = nil
  return true
end

function proxy.rename(from, to)
  return false
end

proxy.close = nop

function proxy.open(path, mode)
  checkArg(1, path, "string")

  local handle = proxy.points[path]
  if not handle then return nil, "device point [" .. path .. "] does not exist" end

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

function proxy.read(h,...)
  return h:read(...)
end

function proxy.seek(h,...)
  return h:seek(...)
end

function proxy.write(h,...)
  return h:write(...)
end

function proxy.create(path, handle)
  handle.close = handle.close or nop
  proxy.points[path] = handle
  return true
end

proxy.create("null", {write = nop})
proxy.create("random", {read = function(_,n)
  local chars = {}
  for i=1,n do
    table.insert(chars,string.char(math.random(0,255)))
  end
  return table.concat(chars)
end})

return proxy
