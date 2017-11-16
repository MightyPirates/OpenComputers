local filesystem = require("filesystem")
local component = require("component")

function filesystem.makeDirectory(path)
  if filesystem.exists(path) then
    return nil, "file or directory with that name already exists"
  end
  local node, rest = filesystem.findNode(path)
  if node.fs and rest then
    local success, reason = node.fs.makeDirectory(rest)
    if not success and not reason and node.fs.isReadOnly() then
      reason = "filesystem is readonly"
    end
    return success, reason
  end
  if node.fs then
    return nil, "virtual directory with that name already exists"
  end
  return nil, "cannot create a directory in a virtual directory"
end

function filesystem.lastModified(path)
  local node, rest, vnode, vrest = filesystem.findNode(path, false, true)
  if not node or not vnode.fs and not vrest then
    return 0 -- virtual directory
  end
  if node.fs and rest then
    return node.fs.lastModified(rest)
  end
  return 0 -- no such file or directory
end

function filesystem.mounts()
  local tmp = {}
  for path,node in pairs(filesystem.fstab) do
    table.insert(tmp, {node.fs,path})
  end
  return function()
    local next = table.remove(tmp)
    if next then return table.unpack(next) end
  end
end

function filesystem.link(target, linkpath)
  checkArg(1, target, "string")
  checkArg(2, linkpath, "string")

  if filesystem.exists(linkpath) then
    return nil, "file already exists"
  end
  local linkpath_parent = filesystem.path(linkpath)
  if not filesystem.exists(linkpath_parent) then
    return nil, "no such directory"
  end
  local linkpath_real, reason = filesystem.realPath(linkpath_parent)
  if not linkpath_real then
    return nil, reason
  end
  if not filesystem.isDirectory(linkpath_real) then
    return nil, "not a directory"
  end

  local _, _, vnode, _ = filesystem.findNode(linkpath_real, true)
  vnode.links[filesystem.name(linkpath)] = target
  return true
end

function filesystem.umount(fsOrPath)
  checkArg(1, fsOrPath, "string", "table")
  local real
  local fs
  local addr
  if type(fsOrPath) == "string" then
    real = filesystem.realPath(fsOrPath)
    addr = fsOrPath
  else -- table
    fs = fsOrPath
  end

  local paths = {}
  for path,node in pairs(filesystem.fstab) do
    if real == path or addr == node.fs.address or fs == node.fs then
      table.insert(paths, path)
    end
  end
  for _,path in ipairs(paths) do
    local node = filesystem.fstab[path]
    filesystem.fstab[path] = nil
    node.fs = nil
    node.parent.children[node.name] = nil
  end
  return #paths > 0
end

function filesystem.size(path)
  local node, rest, vnode, vrest = filesystem.findNode(path, false, true)
  if not node or not vnode.fs and (not vrest or vnode.links[vrest]) then
    return 0 -- virtual directory or symlink
  end
  if node.fs and rest then
    return node.fs.size(rest)
  end
  return 0 -- no such file or directory
end

function filesystem.isLink(path)
  local name = filesystem.name(path)
  local node, rest, vnode, vrest = filesystem.findNode(filesystem.path(path), false, true)
  if not node then return nil, rest end
  local target = vnode.links[name]
  -- having vrest here indicates we are not at the
  -- owning vnode due to a mount point above this point
  -- but we can have a target when there is a link at
  -- the mount point root, with the same name
  if not vrest and target ~= nil then
    return true, target
  end
  return false
end

function filesystem.copy(fromPath, toPath)
  local data = false
  local input, reason = filesystem.open(fromPath, "rb")
  if input then
    local output = filesystem.open(toPath, "wb")
    if output then
      repeat
        data, reason = input:read(1024)
        if not data then break end
        data, reason = output:write(data)
        if not data then data, reason = false, "failed to write" end
      until not data
      output:close()
    end
    input:close()
  end
  return data == nil, reason
end

local function readonly_wrap(proxy)
  checkArg(1, proxy, "table")
  if proxy.isReadOnly() then
    return proxy
  end

  local function roerr() return nil, "filesystem is readonly" end
  return setmetatable({
    rename = roerr,
    open = function(path, mode)
      checkArg(1, path, "string")
      checkArg(2, mode, "string")
      if mode:match("[wa]") then
        return roerr()
      end
      return proxy.open(path, mode)
    end,
    isReadOnly = function()
      return true
    end,
    write = roerr,
    setLabel = roerr,
    makeDirectory = roerr,
    remove = roerr,
  }, {__index=proxy})
end

local function bind_proxy(path)
  local real, reason = filesystem.realPath(path)
  if not real then
    return nil, reason
  end
  if not filesystem.isDirectory(real) then
    return nil, "must bind to a directory"
  end
  local real_fs, real_fs_path = filesystem.get(real)
  if real == real_fs_path then
    return real_fs
  end
  -- turn /tmp/foo into foo
  local rest = real:sub(#real_fs_path + 1)
  local function wrap_relative(fp)
    return function(path, ...)
      return fp(filesystem.concat(rest, path), ...)
    end
  end
  local bind = {
    type = "filesystem_bind",
    address = real,
    isReadOnly = real_fs.isReadOnly,
    list = wrap_relative(real_fs.list),
    isDirectory = wrap_relative(real_fs.isDirectory),
    size = wrap_relative(real_fs.size),
    lastModified = wrap_relative(real_fs.lastModified),
    exists = wrap_relative(real_fs.exists),
    open = wrap_relative(real_fs.open),
    remove = wrap_relative(real_fs.remove),
    read = real_fs.read,
    write = real_fs.write,
    close = real_fs.close,
    getLabel = function() return "" end,
    setLabel = function() return nil, "cannot set the label of a bind point" end,
  }
  return bind
end

filesystem.internal = {}
function filesystem.internal.proxy(filter, options)
  checkArg(1, filter, "string")
  checkArg(2, options, "table", "nil")
  options = options or {}
  local address, proxy, reason
  if options.bind then
    proxy, reason = bind_proxy(filter)
  else
    -- no options: filter should be a label or partial address
    for c in component.list("filesystem", true) do
      if component.invoke(c, "getLabel") == filter then
        address = c
        break
      end
      if c:sub(1, filter:len()) == filter then
        address = c
        break
      end
    end
    if not address then
      return nil, "no such file system"
    end
    proxy, reason = component.proxy(address)
  end
  if not proxy then
    return proxy, reason
  end
  if options.readonly then
    proxy = readonly_wrap(proxy)
  end
  return proxy
end
