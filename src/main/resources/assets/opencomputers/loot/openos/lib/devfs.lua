local fs = require("filesystem")
local comp = require("component")

local function new_node(parent, name, is_dir, proxy)
  local node = {parent=parent, name=name, is_dir=is_dir, proxy=proxy}
  if not proxy then
    node.children = {}
  end
  return node
end

local function new_devfs_dir(name)
  local sys = {}
  sys.mtab = new_node(nil, name or "/", true)

  -- returns: dir, point or path
  -- node (table): the handler responsible for the path
  --   this is essentially the device filesystem that is registered for the given path
  -- point (string): the point name (like a file name)
  function sys.findNode(path, create)
    checkArg(1, path, "string")
    local segments = fs.segments(path)
    local node = sys.mtab
    while #segments > 0 do
      local name = table.remove(segments, 1)
      local prev_path = path
      path = table.concat(segments, "/")

      if not node.children[name] then
        if not create then
          path = prev_path
          break
        end
        node.children[name] = new_node(node, name, true, false)
      end

      node = node.children[name]

      if node.proxy then -- if there is a proxy handler we stop searching here
        break
      end
    end

    -- only dirs can have trailing path
    -- trailing path on a dev point (file) not allowed
    if path == "" or node.is_dir and node.proxy then
      return node, path
    end
  end

  function sys.invoke(method, path, ...)
    local node, rest = sys.findNode(path)
    if not node or -- not found
       rest == "" and node.is_dir or -- path is dir
       not node.proxy[method] then -- optional method
      return 0
    end
    -- proxy could be a file, which doesn't take an argument, but it can be ignored if passed
    return node.proxy[method](rest)
  end

  function sys.size(path)
    return sys.invoke("size", path)
  end

  function sys.lastModified(path)
    return sys.invoke("lastModified", path)
  end

  function sys.isDirectory(path)
    local node, rest = sys.findNode(path)
    if not node then
      return
    end

    if rest == "" then
      return node.is_dir
    elseif node.proxy then
      return node.proxy.isDirectory(rest)
    end
  end

  function sys.open(path, mode)
    checkArg(1, path, "string")
    checkArg(2, mode, "string", "nil")

    if not sys.exists(path) then
      return nil, path.." file not found"
    elseif sys.isDirectory(path) then
      return nil, path.." is a directory"
    end

    mode = mode or "r"
    -- everything at this level should be a binary open
    mode = mode:gsub("b", "")

    if not ({a=true,w=true,r=true})[mode] then
      return nil, "invalid mode"
    end

    local node, rest = sys.findNode(path)
    -- there must be a node, else exists would have failed

    local args = {}
    if rest ~= "" then
      -- having more rest means we expect the proxy fs to open the point
      args[1] = rest
    end
    args[#args+1] = mode

    return node.proxy.open(table.unpack(args))
  end

  function sys.list(path)
    local node, rest = sys.findNode(path)
    if not node or (rest ~= "" and not node.is_dir) then-- not found
      return {}
    elseif rest == "" and not node.is_dir then -- path is file
      return {path}
    elseif node.proxy then
      -- proxy could be a file, which doesn't take an argument, but it can be ignored if passed
      return node.proxy.list(rest)
    end

    -- rest == "" and node.is_dir
    local keys = {}
    for k in pairs(node.children) do
      table.insert(keys, k)
    end
    return keys
  end

  function sys.remove(path)
    return nil, "cannot remove devfs files or directories"
    --checkArg(1, path, "string")

    --if path == "" then
    --  return nil, "no such file or directory"
    --end

    --if not sys.exists(path) then
    --  return nil, path.." file not found"
    --end

    --local node, rest = sys.findNode(path)

    --if rest ~= "" then -- if rest is not resolved, this isn't our path
    --  return node.proxy.remove(rest)
    --end

    --node.parent.children[node.name] = nil
  end

  function sys.exists(path)
    checkArg(1, path, "string")
    local node, rest = sys.findNode(path)

    if not node then
      return false
    elseif rest == "" then
      return true
    else
      return node.proxy.exists(rest)
    end
  end

  function sys.create(path, handler)
    if sys.exists(path) then
      return nil, "path already exists"
    end

    local segments = fs.segments(path)
    local target = table.remove(segments)
    path = table.concat(segments, "/")

    if not target or target == "" then
      return nil, "missing argument"
    end

    local node, rest = sys.findNode(path, true)
    if rest ~= "" then
      return node.proxy.create(rest, handler)
    end
    node.children[target] = new_node(node, target, not not handler.list, handler)
    return true
  end

  return sys
end

local devfs = new_devfs_dir()

local bfd = "bad file descriptor"

function devfs.setLabel(value)
  error("drive does not support labeling")
end

function devfs.makeDirectory(path)
  return false, "to create dirs in devfs use devfs.create"
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

-- devfs.create creates a new dev point at path
-- devfs is mounted to /sys by default, and /dev is a symlink to /sys/dev. If you want a devfs point to show up in /dev, specify a path here as "/dev/your_path")
-- the handler can be a single file dev file (called a point), or a devfs dir [which allows it to list its own dynamic list of points and dirs]
-- note: devfs dirs that list their own child dirs will have to handle directory queries on their own, such as list() and open(path, mode)

-- A handler represents a directory IF it defines list(), which returns a string array of the point names
-- a directory handler acts like simplified filesystem of its own.
-- note: that when creating new devfs points or dirs, devfs.create will not traverse into dynamic directory children of dev mount points
-- Meaning, if you create a devfs dir, which returns dirs children of its own, devfs.create does not support creating dev points
-- on those children

-- see new_devfs_dir() -- it might work for you, /dev uses it

-- Also note, your own devfs dirs may implement open() however they like -- devfs points' open() is called by the devfs library but dynamic
-- dir act as their own library for their own points

-- ### devfs point methods ###
-- Required
  -- open(mode: string []) file: returns new file handle for point (see "devfs point handle methods")
-- Optional
  -- size(path) number

-- ### devfs point handle instance methods ###
-- Required
  -- + technicaly, one of the following is not required when the mode is for the other (e.g. no read when in write mode)
  -- write(self, value, ...) boolean: writes each value (params list) and returns success
  -- read(self, n: number) string: return string of n bytes, nil when no more bytes available
-- Optional
  -- seek(self, whence [string], offset [number]) number: move file handle from whence by offset, return offset result
  -- close(self) boolean: close the file handle. Note that if your open method allocated resources, you'll need to release them in close

-- ### devfs dir methods ###
-- Required
  -- list() string[]: return list of child point names
    -- if you use new_devfs_dir, set metatable on .points with __pairs and __index if you want a dynamic list of files
  -- open(path, mode) file (table): return a file handle to path (path is relative)
    -- it would be nice to make open() optional, but devfs doesn't know where you might store your point handlers, if you even have any
-- Optional
  -- size(path) number
  -- lastModified(path) number
  -- isDirectory(path) boolean -- default returns false. Having dynamic dirs is considered advanced
  -- remove(path) boolean
  -- rename(path) boolean
  -- exists(path) boolean -- default checks path against list() results

-- /dev is a special handler

local function devfs_load(key)
  return require("tools/devfs/" .. key)
end

devfs.create("null", devfs_load("null"))
devfs.create("random", devfs_load("random"))
devfs.create("eeprom", devfs_load("eeprom"))
devfs.create("eeprom-data", devfs_load("eeprom-data"))

return devfs
