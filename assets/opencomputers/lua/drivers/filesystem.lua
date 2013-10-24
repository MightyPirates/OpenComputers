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

driver.filesystem = {}

function driver.filesystem.canonical(path)
  local result = table.concat(segments(path), "/")
  if path:usub(1, 1) == "/" then
    return "/" .. result
  else
    return result
  end
end

function driver.filesystem.concat(pathA, pathB)
  return driver.filesystem.canonical(pathA .. "/" .. pathB)
end

function driver.filesystem.name(path)
  local parts = segments(path)
  return parts[#parts]
end

function driver.filesystem.mount(fs, path)
  if fs and path then
    checkArg(1, fs, "string")
    local node = findNode(path, true)
    if node.fs then
      return nil, "another filesystem is already mounted here"
    end
    node.fs = fs
  else
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
end

function driver.filesystem.umount(fsOrPath)
  local node, rest = findNode(fsOrPath)
  if not rest and node.fs then
    node.fs = nil
    removeEmptyNodes(node)
    return true
  else
    local queue = {mtab}
    for fs, path in driver.filesystem.mount() do
      if fs == fsOrPath then
        local node = findNode(path)
        node.fs = nil
        removeEmptyNodes(node)
        return true
      end
    end
  end
end

-------------------------------------------------------------------------------

function driver.filesystem.label(fs, label)
  if type(label) == "string" then
    return send(fs, "fs.label=", label)
  end
  return send(fs, "fs.label")
end

-------------------------------------------------------------------------------

function driver.filesystem.spaceTotal(path)
  local node, rest = findNode(path)
  if node.fs then
    return send(node.fs, "fs.spaceTotal")
  else
    return nil, "no such device"
  end
end

function driver.filesystem.spaceUsed(path)
  local node, rest = findNode(path)
  if node.fs then
    return send(node.fs, "fs.spaceUsed")
  else
    return nil, "no such device"
  end
end

-------------------------------------------------------------------------------

function driver.filesystem.exists(path)
  local node, rest = findNode(path)
  if not rest then -- virtual directory
    return true
  end
  if node.fs then
    return send(node.fs, "fs.exists", rest)
  end
end

function driver.filesystem.size(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return send(node.fs, "fs.size", rest)
  end
  return 0 -- no such file or directory or it's a virtual directory
end

function driver.filesystem.isDirectory(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return send(node.fs, "fs.isDirectory", rest)
  else
    return not rest or rest:ulen() == 0
  end
end

function driver.filesystem.lastModified(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return send(node.fs, "fs.lastModified", rest)
  end
  return 0 -- no such file or directory or it's a virtual directory
end

function driver.filesystem.dir(path)
  local node, rest = findNode(path)
  if not node.fs and rest then
    return nil, "no such file or directory"
  end
  local result
  if node.fs then
    result = table.pack(send(node.fs, "fs.dir", rest or ""))
    if not result[1] and result[2] then
      return nil, result[2]
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

-------------------------------------------------------------------------------

function driver.filesystem.makeDirectory(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return send(node.fs, "fs.makeDirectory", rest)
  end
  return nil, "cannot create a directory in a virtual directory"
end

function driver.filesystem.remove(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return send(node.fs, "fs.remove", rest)
  end
  return nil, "no such non-virtual directory"
end

function driver.filesystem.rename(oldPath, newPath)
  local oldNode, oldRest = findNode(oldPath)
  local newNode, newRest = findNode(newPath)
  if oldNode.fs and oldRest and newNode.fs and newRest then
    if oldNode.fs == newNode.fs then
      return send(oldNode.fs, "fs.rename", oldRest, newRest)
    else
      local result, reason = driver.filesystem.copy(oldPath, newPath)
      if result then
        return driver.filesystem.remove(oldPath)
      else
        return nil, reason
      end
    end
  end
  return nil, "trying to read from or write to virtual directory"
end

function driver.filesystem.copy(fromPath, toPath)
  local input, reason = io.open(fromPath, "rb")
  if not input then
    error(reason)
  end
  local output, reason = io.open(toPath, "wb")
  if not output then
    input:close()
    error(reason)
  end
  repeat
    local buffer, reason = input:read(1024)
    if not buffer and reason then
      error(reason)
    elseif buffer then
      local result, reason = output:write(buffer)
      if not result then
        input:close()
        output:close()
        error(reason)
      end
    end
  until not buffer
  input:close()
  output:close()
  return true
end

-------------------------------------------------------------------------------

-------------------------------------------------------------------------------

local fileStream = {}

function fileStream:close()
  send(self.fs, "fs.close", self.handle)
  self.handle = nil
end

function fileStream:read(n)
  if not self.handle then
    return nil, "file is closed"
  end
  return send(self.fs, "fs.read", self.handle, n)
end

function fileStream:seek(whence, offset)
  if not self.handle then
    return nil, "file is closed"
  end
  return send(self.fs, "fs.seek", self.handle, whence, offset)
end

function fileStream:write(str)
  if not self.handle then
    return nil, "file is closed"
  end
  return send(self.fs, "fs.write", self.handle, str)
end

-------------------------------------------------------------------------------

function driver.filesystem.open(path, mode)
  mode = tostring(mode or "r")
  checkArg(2, mode, "string")
  assert(({r=true, rb=true, w=true, wb=true, a=true, ab=true})[mode],
    "bad argument #2 (r[b], w[b] or a[b] expected, got " .. mode .. ")")

  local node, rest = findNode(path)
  if not node.fs or not rest then
    return nil, "file not found"
  end

  local handle, reason = send(node.fs, "fs.open", rest, mode)
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
      send(fs, "fs.close", handle)
    end
    event.timer(0, close)
  end
  local metatable = {__index = fileStream,
                     __gc = cleanup,
                     __metatable = "filestream"}
  return setmetatable(stream, metatable)
end
