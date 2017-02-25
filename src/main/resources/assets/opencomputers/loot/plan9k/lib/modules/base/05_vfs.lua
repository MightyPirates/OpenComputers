local unicode = unicode

local filesystem, fileStream = {}, {}
local isAutorunEnabled = nil
mtab = {name="", children={}, links={}}

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

local function saveConfig()
  local root = filesystem.get("/")
  if root and not root.isReadOnly() then
    filesystem.makeDirectory("/etc")
    local f = io.open("/etc/filesystem.cfg", "w")
    if f then
      f:write("autorun="..tostring(isAutorunEnabled))
      f:close()
    end
  end
end

local function resolve(path)
    if unicode.sub(path, 1, 1) == "/" then
        return filesystem.canonical(path)
    elseif unicode.sub(path, 1, 2) == "~/" then
        return filesystem.concat(kernel.modules.threading.currentThread.env.HOME or "/", path:sub(2))
    else
        return filesystem.concat(kernel.modules.threading.currentThread.env.PWD or "/", path)
    end
end

local function findNode(path, create, depth)
  checkArg(1, path, "string")
  depth = depth or 0
  if depth > 100 then
    error("link cycle detected")
  end
  local parts = segments(path)
  local node = mtab
  while #parts > 0 do
    local part = parts[1]
    if not node.children[part] then
      if node.links[part] then
        return findNode(filesystem.concat(node.links[part], table.concat(parts, "/", 2)), create, depth + 1)
      else
        if create then
          node.children[part] = {name=part, parent=node, children={}, links={}}
        else
          local vnode, vrest = node, table.concat(parts, "/")
          local rest = vrest
          while node and not node.fs do
            rest = filesystem.concat(node.name, rest)
            node = node.parent
          end
          return node, rest, vnode, vrest
        end
      end
    end
    node = node.children[part]
    table.remove(parts, 1)
  end
  local vnode, vrest = node, nil
  local rest = nil
  while node and not node.fs do
    rest = rest and filesystem.concat(node.name, rest) or node.name
    node = node.parent
  end
  return node, rest, vnode, vrest
end

local function removeEmptyNodes(node)
  while node and node.parent and not node.fs and not next(node.children) and not next(node.links) do
    node.parent.children[node.name] = nil
    node = node.parent
  end
end

-------------------------------------------------------------------------------

filesystem.resolve = resolve

function filesystem.isAutorunEnabled()
  if isAutorunEnabled == nil then
    local env = {}
    local config = loadfile("/etc/filesystem.cfg", nil, env)
    if config then
      pcall(config)
      isAutorunEnabled = not not env.autorun
    else
      isAutorunEnabled = true
    end
    saveConfig()
  end
  return isAutorunEnabled
end

function filesystem.setAutorunEnabled(value)
  checkArg(1, value, "boolean")
  isAutorunEnabled = value
  saveConfig()
end

function filesystem.segments(path)
  return segments(path)
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
  local node, rest = findNode(resolve(path))
  if node.fs then
    local proxy = node.fs
    path = ""
    while node and node.parent do
      path = filesystem.concat(node.name, path)
      node = node.parent
    end
    path = filesystem.canonical(path)
    if path ~= "/" then
      path = "/" .. path
    end
    return proxy, path
  end
  return nil, "no such file system"
end

function filesystem.isLink(path)
  local node, rest, vnode, vrest = findNode(filesystem.path(resolve(path)))
  if not vrest and vnode.links[filesystem.name(path)] ~= nil then
    return true, vnode.links[filesystem.name(path)]
  end
  return false
end

function filesystem.link(target, linkpath)
  checkArg(1, target, "string")
  checkArg(2, linkpath, "string")
  
  linkpath = resolve(linkpath)
  if filesystem.exists(linkpath) then
    return nil, "file already exists"
  end

  local node, rest, vnode, vrest = findNode(filesystem.path(linkpath), true)
  vnode.links[filesystem.name(linkpath)] = target
  return true
end

function filesystem.mount(fs, path)
  checkArg(1, fs, "string", "table")
  if type(fs) == "string" then
    fs = filesystem.proxy(fs)
  end
  assert(type(fs) == "table", "bad argument #1 (file system proxy or address expected)")
  checkArg(2, path, "string")

  path = resolve(path)
  if path ~= "/" and filesystem.exists(path) then
    return nil, "file already exists"
  end

  local node, rest, vnode, vrest = findNode(path, true)
  if vnode.fs then
    return nil, "another filesystem is already mounted here"
  end
  vnode.fs = fs
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
    while #queue > 0 do
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

function filesystem.path(path)
  local parts = segments(path)
  local result = table.concat(parts, "/", 1, #parts - 1) .. "/"
  if unicode.sub(path, 1, 1) == "/" and unicode.sub(result, 1, 1) ~= "/" then
    return "/" .. result
  else
    return result
  end
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
    local node, rest, vnode, vrest = findNode(fsOrPath)
    if not vrest and vnode.fs then
      vnode.fs = nil
      removeEmptyNodes(vnode)
      return true
    end
  end
  local address = type(fsOrPath) == "table" and fsOrPath.address or fsOrPath
  local result = false
  for proxy, path in filesystem.mounts() do
    local addr = type(proxy) == "table" and proxy.address or proxy
    if string.sub(addr, 1, address:len()) == address then
      local node, rest, vnode, vrest = findNode(path)
      vnode.fs = nil
      removeEmptyNodes(vnode)
      result = true
    end
  end
  return result
end

function filesystem.exists(path)
  local node, rest, vnode, vrest = findNode(resolve(path))
  if not vrest or vnode.links[vrest] then -- virtual directory or symbolic link
    return true
  end
  if node and node.fs then
    return node.fs.exists(rest)
  end
  return false
end

function filesystem.size(path)
  local node, rest, vnode, vrest = findNode(resolve(path))
  if not vnode.fs and (not vrest or vnode.links[vrest]) then
    return 0 -- virtual directory or symlink
  end
  if node.fs and rest then
    return node.fs.size(rest)
  end
  return 0 -- no such file or directory
end

function filesystem.isDirectory(path)
  local node, rest, vnode, vrest = findNode(resolve(path))
  if not vnode.fs and not vrest then
    return true -- virtual directory
  end
  if node.fs then
    return not rest or node.fs.isDirectory(rest)
  end
  return false
end

function filesystem.lastModified(path)
  local node, rest, vnode, vrest = findNode(resolve(path))
  if not vnode.fs and not vrest then
    return 0 -- virtual directory
  end
  if node.fs and rest then
    return node.fs.lastModified(rest)
  end
  return 0 -- no such file or directory
end

function filesystem.list(path)
  local node, rest, vnode, vrest = findNode(resolve(path))
  if not vnode.fs and vrest and not (node and node.fs) then
    return nil, "no such file or directory"
  end
  local result, reason
  if node and node.fs then
    result, reason = node.fs.list(rest or "")
  end
  result = result or {}
  if not vrest then
    for k in pairs(vnode.children) do
      table.insert(result, k .. "/")
    end
    for k in pairs(vnode.links) do
      table.insert(result, k)
    end
  end
  table.sort(result)
  local i, f = 1, nil
  while i <= #result do
    if result[i] == f then
      table.remove(result, i)
    else
      f = result[i]
      i = i + 1
    end
  end
  local i = 0
  return function()
    i = i + 1
    return result[i]
  end
end

function filesystem.makeDirectory(path)
  if filesystem.exists(resolve(path)) then
    return nil, "file or directory with that name already exists"
  end
  local node, rest = findNode(resolve(path))
  if node.fs and rest then
    return node.fs.makeDirectory(rest)
  end
  if node.fs then
    return nil, "virtual directory with that name already exists"
  end
  return nil, "cannot create a directory in a virtual directory"
end

function filesystem.remove(path)
  local function removeVirtual()
    local node, rest, vnode, vrest = findNode(filesystem.path(resolve(path)))
    local name = filesystem.name(resolve(path))
    if vnode.children[name] then
      vnode.children[name] = nil
      removeEmptyNodes(vnode)
      return true
    elseif vnode.links[name] then
      vnode.links[name] = nil
      removeEmptyNodes(vnode)
      return true
    end
    return false
  end
  local function removePhysical()
    node, rest = findNode(resolve(path))
    if node.fs and rest then
      return node.fs.remove(rest)
    end
    return false
  end
  local success = removeVirtual()
  success = removePhysical() or success -- Always run.
  if success then return true
  else return nil, "no such file or directory"
  end
end

function filesystem.rename(oldPath, newPath)
  oldPath = resolve(oldPath)
  newPath = resolve(newPath)
  if filesystem.isLink(oldPath) then
    local node, rest, vnode, vrest = findNode(filesystem.path(oldPath))
    local target = vnode.links[filesystem.name(oldPath)]
    local result, reason = filesystem.link(target, newPath)
    if result then
      filesystem.remove(oldPath)
    end
    return result, reason
  else
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
end

function filesystem.copy(fromPath, toPath)
  fromPath = resolve(fromPath)
  toPath = resolve(toPath)
  if filesystem.isDirectory(fromPath) then
    return nil, "cannot copy folders"
  end
  local input, reason = kernel.modules.io.io.open(fromPath, "rb")
  if not input then
    return nil, reason, "open input"
  end
  local output, reason = kernel.modules.io.io.open(toPath, "wb")
  if not output then
    input:close()
    return nil, reason, "open output"
  end
  repeat
    local buffer, reason = input:read(1024)
    if not buffer and reason then
      return nil, reason, "read input"
    elseif buffer then
      local result, reason = output:write(buffer)
      if not result then
        input:close()
        output:close()
        return nil, reason, "write to output"
      end
    end
  until not buffer
  input:close()
  output:close()
  return true
end

function fileStream:close()
  if self.handle then
    self.fs.close(self.handle)
    self.handle = nil
  end
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

  local node, rest = findNode(resolve(path))
  if not node.fs or not rest then
    return nil, "file not found"
  end

  local handle, reason = node.fs.open(rest, mode)
  if not handle then
    return nil, reason
  end

  local stream = {fs = node.fs, handle = handle}

  local function cleanup(self)
    if not self.handle then return end
    pcall(self.fs.close, self.handle)
  end
  local metatable = {__index = fileStream,
                     __gc = cleanup,
                     __metatable = "filestream"}
  return setmetatable(stream, metatable)
end

-------------------------------------------------------------------------------

for k,v in pairs(filesystem) do
    _G[k] = v
end

