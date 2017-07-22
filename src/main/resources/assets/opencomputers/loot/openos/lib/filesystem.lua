local component = require("component")
local unicode = require("unicode")

local filesystem = {}
local isAutorunEnabled = nil
local mtab = {name="", children={}, links={}}
local fstab = {}

local function segments(path)
  local parts = {}
  for part in path:gmatch("[^\\/]+") do
    local current, up = part:find("^%.?%.$")
    if current then
      if up == 2 then
        table.remove(parts)
      end
    else
      table.insert(parts, part)
    end
  end
  return parts
end

local function saveConfig()
  local root = filesystem.get("/")
  if root and not root.isReadOnly() then
    local f = filesystem.open("/etc/filesystem.cfg", "w")
    if f then
      f:write("autorun="..tostring(isAutorunEnabled))
      f:close()
    end
  end
end

local function findNode(path, create, resolve_links)
  checkArg(1, path, "string")
  local visited = {}
  local parts = segments(path)
  local ancestry = {}
  local node = mtab
  local index = 1
  while index <= #parts do
    local part = parts[index]
    ancestry[index] = node
    if not node.children[part] then
      local link_path = node.links[part]
      if link_path then
        if not resolve_links and #parts == index then break end

        if visited[path] then
          return nil, string.format("link cycle detected '%s'", path)
        end
        -- the previous parts need to be conserved in case of future ../.. link cuts
        visited[path] = index
        local pst_path = "/" .. table.concat(parts, "/", index + 1)
        local pre_path

        if link_path:match("^[^/]") then
          pre_path = table.concat(parts, "/", 1, index - 1) .. "/"
          local link_parts = segments(link_path)
          local join_parts = segments(pre_path .. link_path)
          local back = (index - 1 + #link_parts) - #join_parts
          index = index - back
          node = ancestry[index]
        else
          pre_path = ""
          index = 1
          node = mtab
        end

        path = pre_path .. link_path .. pst_path
        parts = segments(path)
        part = nil -- skip node movement
      elseif create then
        node.children[part] = {name=part, parent=node, children={}, links={}}
      else
        break
      end
    end
    if part then
      node = node.children[part]
      index = index + 1
    end
  end

  local vnode, vrest = node, #parts >= index and table.concat(parts, "/", index)
  local rest = vrest
  while node and not node.fs do
    rest = rest and filesystem.concat(node.name, rest) or node.name
    node = node.parent
  end
  return node, rest, vnode, vrest
end

-------------------------------------------------------------------------------

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

function filesystem.canonical(path)
  local result = table.concat(segments(path), "/")
  if unicode.sub(path, 1, 1) == "/" then
    return "/" .. result
  else
    return result
  end
end

function filesystem.concat(...)
  local set = table.pack(...)
  for index, value in ipairs(set) do
    checkArg(index, value, "string")
  end
  return filesystem.canonical(table.concat(set, "/"))
end

function filesystem.get(path)
  local node = findNode(path)
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

function filesystem.realPath(path)
  checkArg(1, path, "string")
  local node, rest = findNode(path, false, true)
  if not node then return nil, rest end
  local parts = {rest or nil}
  repeat
    table.insert(parts, 1, node.name)
    node = node.parent
  until not node
  return table.concat(parts, "/")
end

function filesystem.mount(fs, path)
  checkArg(1, fs, "string", "table")
  if type(fs) == "string" then
    fs = filesystem.proxy(fs)
  end
  assert(type(fs) == "table", "bad argument #1 (file system proxy or address expected)")
  checkArg(2, path, "string")

  local real
  if not mtab.fs then
    if path == "/" then
      real = path
    else
      return nil, "rootfs must be mounted first"
    end
  else
    local why
    real, why = filesystem.realPath(path)
    if not real then
      return nil, why
    end

    if filesystem.exists(real) then
      return nil, "file already exists"
    end
  end

  local fsnode
  if fstab[real] then
    return nil, "another filesystem is already mounted here"
  end
  for _,node in pairs(fstab) do
    if node.fs.address == fs.address then
      fsnode = node
      break
    end
  end

  if not fsnode then
    fsnode = select(3, findNode(real, true))
    -- allow filesystems to intercept their own nodes
    fs.fsnode = fsnode
  else
    local pwd = filesystem.path(real)
    local parent = select(3, findNode(pwd, true))
    local name = filesystem.name(real)
    fsnode = setmetatable({name=name,parent=parent},{__index=fsnode})
    parent.children[name] = fsnode
  end

  fsnode.fs = fs
  fstab[real] = fsnode

  return true
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
  checkArg(1, path, "string")
  local parts = segments(path)
  return parts[#parts]
end

function filesystem.proxy(filter)
  checkArg(1, filter, "string")
  local address
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
  return component.proxy(address)
end

function filesystem.exists(path)
  if not filesystem.realPath(filesystem.path(path)) then
    return false
  end 
  local node, rest, vnode, vrest = findNode(path)
  if not vrest or vnode.links[vrest] then -- virtual directory or symbolic link
    return true
  elseif node and node.fs then
    return node.fs.exists(rest)
  end
  return false
end

function filesystem.isDirectory(path)
  local real, reason = filesystem.realPath(path)
  if not real then return nil, reason end
  local node, rest, vnode, vrest = findNode(real)
  if not vnode.fs and not vrest then
    return true -- virtual directory (mount point)
  end
  if node.fs then
    return not rest or node.fs.isDirectory(rest)
  end
  return false
end

function filesystem.list(path)
  local node, rest, vnode, vrest = findNode(path, false, true)
  local result = {}
  if node then
    result = node.fs and node.fs.list(rest or "") or {}
    -- `if not vrest` indicates that vnode reached the end of path
    -- in other words, vnode[children, links] represent path
    if not vrest then
      for k,n in pairs(vnode.children) do
        if not n.fs or fstab[filesystem.concat(path, k)] then
          table.insert(result, k .. "/")
        end
      end
      for k in pairs(vnode.links) do
        table.insert(result, k)
      end
    end
  end
  local set = {}
  for _,name in ipairs(result) do
    set[filesystem.canonical(name)] = name
  end
  return function()
    local key, value = next(set)
    set[key or false] = nil
    return value
  end
end

function filesystem.remove(path)
  return require("tools/fsmod").remove(path, findNode)
end

function filesystem.rename(oldPath, newPath)
  return require("tools/fsmod").rename(oldPath, newPath, findNode)
end

function filesystem.open(path, mode)
  checkArg(1, path, "string")
  mode = tostring(mode or "r")
  checkArg(2, mode, "string")

  assert(({r=true, rb=true, w=true, wb=true, a=true, ab=true})[mode],
    "bad argument #2 (r[b], w[b] or a[b] expected, got " .. mode .. ")")

  local node, rest = findNode(path, false, true)
  if not node then
    return nil, rest
  end
  if not node.fs or not rest or (({r=true,rb=true})[mode] and not node.fs.exists(rest)) then
    return nil, "file not found"
  end

  local handle, reason = node.fs.open(rest, mode)
  if not handle then
    return nil, reason
  end

  local function create_handle_method(key)
    return function(self, ...)
      if not self.handle then
        return nil, "file is closed"
      end
      return self.fs[key](self.handle, ...)
    end
  end

  local stream =
  {
    fs = node.fs,
    handle = handle,
    close = function(self)
      if self.handle then
        self.fs.close(self.handle)
        self.handle = nil
      end
    end
  }
  stream.read = create_handle_method("read")
  stream.seek = create_handle_method("seek")
  stream.write = create_handle_method("write")
  return stream
end

filesystem.findNode = findNode
filesystem.segments = segments
filesystem.fstab = fstab

-------------------------------------------------------------------------------

return filesystem
