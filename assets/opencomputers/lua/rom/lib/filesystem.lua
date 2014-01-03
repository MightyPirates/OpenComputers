local filesystem, fileStream = {}, {}
local isAutorunEnabled = true
local mtab = {children={}}

local function segments(path)
  path = path:gsub("\\", "/")
  repeat local n; path, n = path:gsub("//", "/") until n == 0
  local parts = {}
  for part in path:gmatch("[^/]+") do
    table.insert(parts, part)
  end
  local i = 1
  while i <= #parts do
    if parts[i] == "." then
      table.remove(parts, i)
    elseif parts[i] == ".." then
      table.remove(parts, i)
      i = i - 1
      if i > 0 then
        table.remove(parts, i)
      else
        i = 1
      end
    else
      i = i + 1
    end
  end
  return parts
end

local function findNode(path, create)
  checkArg(1, path, "string")
  local parts = segments(path)
  local node = mtab
  for i = 1, #parts do
    if not node.children[parts[i]] then
      if create then
        node.children[parts[i]] = {children={}, parent=node}
      else
        return node, table.concat(parts, "/", i)
      end
    end
    node = node.children[parts[i]]
  end
  return node
end

local function removeEmptyNodes(node)
  while node and node.parent and not node.fs and not next(node.children) do
    for k, c in pairs(node.parent.children) do
      if c == node then
        node.parent.children[k] = nil
        break
      end
    end
    node = node.parent
  end
end

-------------------------------------------------------------------------------

function filesystem.isAutorunEnabled()
  return isAutorunEnabled
end

function filesystem.setAutorunEnabled(value)
  checkArg(1, value, "boolean")
  isAutorunEnabled = value
end

function filesystem.canonical(path)
  local result = table.concat(segments(path), "/")
  if unicode.sub(path, 1, 1) == "/" then
    return "/" .. result
  else
    return result
  end
end

function filesystem.concat(pathA, pathB, ...)
  checkArg(1, pathA, "string")
  local function concat(n, a, b, ...)
    if not b then
      return a
    end
    checkArg(n, b, "string")
    return concat(n + 1, a .. "/" .. b, ...)
  end
  return filesystem.canonical(concat(2, pathA, pathB, ...))
end

function filesystem.get(path)
  local node, rest = findNode(path)
  if node.fs then
    local proxy = node.fs
    path = "/"
    while node.parent do
      for name, child in pairs(node.parent.children) do
        if child == node then
          path = "/" .. name .. path
          break
        end
      end
      node = node.parent
    end
    return proxy, filesystem.canonical(path)
  end
  return nil, "no such file system"
end

function filesystem.mount(fs, path)
  checkArg(1, fs, "string", "table")
  if type(fs) == "string" then
    fs = filesystem.proxy(fs)
  end
  assert(type(fs) == "table", "bad argument #1 (file system proxy or address expected)")
  checkArg(2, path, "string")

  local node = findNode(path, true)
  if node.fs then
    return nil, "another filesystem is already mounted here"
  end
  node.fs = fs
  return true
end

function filesystem.mounts()
  local function path(node)
    local result = "/"
    while node and node.parent do
      for name, child in pairs(node.parent.children) do
        if child == node then
          result = "/" .. name .. result
          break
        end
      end
      node = node.parent
    end
    return result
  end
  local queue = {mtab}
  return function()
    if #queue == 0 then
      return nil
    else
      while true do
        local node = table.remove(queue)
        for _, child in pairs(node.children) do
          table.insert(queue, child)
        end
        if node.fs then
          return node.fs, path(node)
        end
      end
    end
  end
end

function filesystem.path(path)
  local parts = segments(path)
  return table.concat(parts, "/", 1, #parts - 1) .. "/"
end

function filesystem.name(path)
  local parts = segments(path)
  return parts[#parts]
end

function filesystem.proxy(filter)
  checkArg(1, filter, "string")
  local address
  for c in component.list("filesystem") do
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
  return component.proxy(address)
end

function filesystem.umount(fsOrPath)
  checkArg(1, fsOrPath, "string", "table")
  if type(fsOrPath) == "string" then
    local node, rest = findNode(fsOrPath)
    if not rest and node.fs then
      node.fs = nil
      removeEmptyNodes(node)
      return true
    end
  end
  local function unmount(address)
    local queue = {mtab}
    for proxy, path in filesystem.mounts() do
      if string.sub(proxy.address, 1, address:len()) == address then
        local node = findNode(path)
        node.fs = nil
        removeEmptyNodes(node)
        return true
      end
    end
  end
  local address = type(fsOrPath) == "table" and fsOrPath.address or fsOrPath
  local result = false
  while unmount(address) do result = true end
  return result
end

function filesystem.exists(path)
  local node, rest = findNode(path)
  if not rest then -- virtual directory
    return true
  end
  if node.fs then
    return node.fs.exists(rest)
  end
  return false
end

function filesystem.size(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return node.fs.size(rest)
  end
  return 0 -- no such file or directory or it's a virtual directory
end

function filesystem.isDirectory(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return node.fs.isDirectory(rest)
  else
    return not rest or unicode.len(rest) == 0
  end
end

function filesystem.lastModified(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return node.fs.lastModified(rest)
  end
  return 0 -- no such file or directory or it's a virtual directory
end

function filesystem.list(path)
  local node, rest = findNode(path)
  if not node.fs and rest then
    return nil, "no such file or directory"
  end
  local result, reason
  if node.fs then
    result, reason = node.fs.list(rest or "")
    if not result then
      return nil, reason or "no such directory"
    end
  else
    result = {}
  end
  if not rest then
    for k, _ in pairs(node.children) do
      table.insert(result, k .. "/")
    end
  end
  table.sort(result)
  local i = 0
  return function()
    i = i + 1
    return result[i]
  end
end

function filesystem.makeDirectory(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return node.fs.makeDirectory(rest)
  end
  if node.fs then
    return nil, "virtual directory with that name already exists"
  end
  return nil, "cannot create a directory in a virtual directory"
end

function filesystem.remove(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return node.fs.remove(rest)
  end
  return nil, "no such non-virtual directory"
end

function filesystem.rename(oldPath, newPath)
  local oldNode, oldRest = findNode(oldPath)
  local newNode, newRest = findNode(newPath)
  if oldNode.fs and oldRest and newNode.fs and newRest then
    if oldNode.fs.address == newNode.fs.address then
      return oldNode.fs.rename(oldRest, newRest)
    else
      local result, reason = filesystem.copy(oldPath, newPath)
      if result then
        return filesystem.remove(oldPath)
      else
        return nil, reason
      end
    end
  end
  return nil, "trying to read from or write to virtual directory"
end

function filesystem.copy(fromPath, toPath)
  if filesystem.isDirectory(fromPath) then
    return nil, "cannot copy folders"
  end
  local input, reason = io.open(fromPath, "rb")
  if not input then
    return nil, reason
  end
  local output, reason = io.open(toPath, "wb")
  if not output then
    input:close()
    return nil, reason
  end
  repeat
    local buffer, reason = input:read(1024)
    if not buffer and reason then
      return nil, reason
    elseif buffer then
      local result, reason = output:write(buffer)
      if not result then
        input:close()
        output:close()
        return nil, reason
      end
    end
  until not buffer
  input:close()
  output:close()
  return true
end

function fileStream:close()
  self.fs.close(self.handle)
  self.handle = nil
end

function fileStream:read(n)
  if not self.handle then
    return nil, "file is closed"
  end
  return self.fs.read(self.handle, n)
end

function fileStream:seek(whence, offset)
  if not self.handle then
    return nil, "file is closed"
  end
  return self.fs.seek(self.handle, whence, offset)
end

function fileStream:write(str)
  if not self.handle then
    return nil, "file is closed"
  end
  return self.fs.write(self.handle, str)
end

function filesystem.open(path, mode)
  checkArg(1, path, "string")
  mode = tostring(mode or "r")
  checkArg(2, mode, "string")
  assert(({r=true, rb=true, w=true, wb=true, a=true, ab=true})[mode],
    "bad argument #2 (r[b], w[b] or a[b] expected, got " .. mode .. ")")

  local node, rest = findNode(path)
  if not node.fs or not rest then
    return nil, "file not found"
  end

  local handle, reason = node.fs.open(rest, mode)
  if not handle then
    return nil, reason
  end

  local stream = {fs = node.fs, handle = handle}

  -- stream:close does a syscall, which yields, and that's not possible in
  -- the __gc metamethod. So we start a timer to do the yield/cleanup.
  local function cleanup(self)
    if not self.handle then return end
    -- save non-gc'ed values as upvalues
    local fs = self.fs
    local handle = self.handle
    local function close()
      fs.close(handle)
    end
    event.timer(0, close)
  end
  local metatable = {__index = fileStream,
                     __gc = cleanup,
                     __metatable = "filestream"}
  return setmetatable(stream, metatable)
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address, componentType)
  if componentType == "filesystem" then
    local proxy = component.proxy(address)
    if proxy then
      local name = address:sub(1, 3)
      while filesystem.exists(filesystem.concat("/mnt", name)) and
            name:len() < address:len() -- just to be on the safe side
      do
        name = address:sub(1, name:len() + 1)
      end
      name = filesystem.concat("/mnt", name)
      filesystem.mount(proxy, name)
      if isAutorunEnabled then
        local result, reason = shell.execute(filesystem.concat(name, "autorun"), _ENV, proxy)
        if not result then
          error (reason)
        end
      end
    end
  end
end

local function onComponentRemoved(_, address, componentType)
  if componentType == "filesystem" then
    if filesystem.get(shell.getWorkingDirectory()).address == address then
      shell.setWorkingDirectory("/")
    end
    filesystem.umount(address)
  end
end

_G.filesystem = filesystem
_G.fs = filesystem

return function()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end
