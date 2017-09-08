local filesystem = require("filesystem")

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

