local fs = require("filesystem")
local text = require("text")

local api = {}

local function new_node(proxy)
  local node = {proxy=proxy}
  if not proxy or not proxy.list then
    node.children = {}
  end
  return node
end

local function array_read(array, separator)
  separator = separator or " "
  local builder = {}
  for _,value in ipairs(array) do
    table.insert(builder, tostring(value))
  end
  return table.concat(builder, separator)
end

local function child_iterator(node)
  -- a node can either list or have children, but not both (see add_child)
  -- a node can be a file, which has a proxy, but no children
  local listed = {}
  if node then
    if node.proxy and node.proxy.list then
      -- list should return a table, not another iterator
      -- the elements in the list are not nodes, but proxies
      -- we have to wrap each entry with a virtual node (a node that is not in a child-parent tree)
      -- list can be a function that returns a table, or the table already
      local list = node.proxy.list
      listed = type(list) == "table" and list or list()
    elseif node.children then
      listed = node.children
    end
  end
  local availables = {}
  for name, item in pairs(listed) do
    if name:len() > 0 then
      if not item.proxy then item = new_node(item) end
      if not item.proxy.isAvailable or item.proxy.isAvailable() then
        availables[name] = item
      end
    end
  end
  return pairs(availables)
end

local function get_child(node, name)
  for child_name, child in child_iterator(node) do
    if child_name == name then
      return child
    end
  end
end

local function add_child(node, name, proxy)
  if not node or node.proxy and node.proxy.list then
    return nil, "cannot add child to listing proxy"
  end

  local child = new_node(proxy)
  node.children[name] = child
  return child
end

local function findNode(path, bCreate)
  local segments = fs.segments(path)
  local node = api.root
  while #segments > 0 do
    local name = table.remove(segments, 1)
    local next = get_child(node, name)
    if not next then
      if bCreate then
        if not add_child(node, name) then
          return nil, "cannot create child node"
        end
      else
        return nil, "no such file or directory"
      end
    end
    node = next or get_child(node, name)
  end
  return node
end

-- devfs api

api.root = new_node()

function api.create(path, proxy)
  checkArg(1, path, "string")
  checkArg(2, proxy, "table", "nil")
  local pwd = fs.path(path)
  local name = fs.name(path)
  if not name then return nil, "invalid devfs path" end
  local pnode, why = findNode(pwd, true)
  if not pnode then
    return nil, why
  end

  if get_child(pnode, name) then
    return nil, "file or directory exists"
  end

  return add_child(pnode, name, proxy)
end

-- the filesystem object as seen from the system mount interface
api.proxy = {}

-- forward declare injector
local inject_dynamic_pairs
local function dynamic_list(path, fsnode)
  local nodes, links, dirs = {}, {}, {}
  local node = findNode(path)
  if node then
    for name,cnode in child_iterator(node) do
      if cnode.proxy and cnode.proxy.link then
        links[name] = cnode.proxy.link
      elseif cnode.proxy and cnode.proxy.list then
        local child = {name=name,parent=fsnode}
        local child_path = path .. "/" .. name
        inject_dynamic_pairs(child, child_path, true)
        dirs[name] = child
      else
        nodes[name] = cnode
      end
    end
  end
  return nodes, links, dirs
end

inject_dynamic_pairs = function(fsnode, path, bStoreUse)
  if getmetatable(fsnode) then return end
  fsnode.children = nil
  fsnode.links = nil
  setmetatable(fsnode,
  {
    __index = function(tbl, key)
      local bLinks = key == "links"
      local bChildren = key == "children"
      if not bLinks and not bChildren then return end
      local _, links, dirs = dynamic_list(path, tbl)
      if bStoreUse then
        tbl.children = dirs
        tbl.links = links
      end
      return bLinks and links or dirs
    end
  })
end

local label_lib = dofile("/lib/core/device_labeling.lua")
label_lib.loadRules()
api.getDeviceLabel = label_lib.getDeviceLabel
api.setDeviceLabel = label_lib.setDeviceLabel

local registered = false
function api.register(public_proxy)
  if registered then return end
  registered = true

  local start_path = "/lib/core/devfs/"
  for starter in fs.list(start_path) do
    local full_path = start_path .. starter
    local _,matched = starter:gsub("%.lua$","")
    if matched > 0 then
      local data = dofile(full_path)
      for name, entry in pairs(data) do
        api.create(name, entry)
      end
    end
  end

  if rawget(public_proxy, "fsnode") then
    inject_dynamic_pairs(public_proxy.fsnode, "")
  end
end

function api.proxy.list(path)
  local result = {}
  for name in pairs(dynamic_list(path, false)) do
    table.insert(result, name)
  end
  return result
end

function api.proxy.isDirectory(path)
  local node = findNode(path)
  return node and node.proxy and node.proxy.list
end

function api.proxy.size(path)
  checkArg(1, path, "string")
  local node = findNode(path)
  if not node or not node.proxy then
    return 0
  end

  local proxy = node.proxy
  if proxy.list then return 0 end
  if proxy.size then return proxy.size() end
  if proxy.open then return 0 end
  if proxy.read then return proxy.read():len() end
  if proxy[1] ~= nil then return array_read(proxy):len() end
  return 0
end

function api.proxy.lastModified()
  return 0
end

function api.proxy.exists(path)
  checkArg(1, path, "string")
  return not not findNode(path)
end

function api.getDevice(path)
  checkArg(1, path, "string")
  local device
  local reason = "no such device"
  local real, why = fs.realPath(require("shell").resolve(path))
  if not real then return nil, why end
  if fs.exists(real) then
    -- we don't have a good way of knowing where dev is mounted still
    -- similar hack in api.proxy.open
    real = fs.path(real) .. (fs.name(real) or "")
    local part, subbed = real:gsub("^/dev/", "")
    if subbed > 0 and part:len() > 0 then
      local node = findNode(part)
      if node and node.proxy then
        -- must be a special device node
        device = node.proxy.device
      end
      if not device then
        reason = "not a device"
      end
    else
      device, reason = fs.get(real)
    end
  end
  return device, reason
end

function api.proxy.open(path, mode)
  checkArg(1, path, "string")
  checkArg(2, mode, "string", "nil")

  mode = mode or "r"
  local bRead = mode:match("[ra]")
  local bWrite = mode:match("[wa]")

  if not bRead and not bWrite then
    return nil, "invalid mode"
  end

  local node, why = findNode(path)
  if not node then
    return nil, why
  elseif not node.proxy or node.proxy.list then
    return nil, "is a directory"
  end

  local proxy = node.proxy

  -- in case someone tries to open a link directly, refer them back to fs
  -- this is an unfortunate pathing hack due to optimizations for memory
  if proxy.link then
    return fs.open("/dev/"..path, mode)
  end

  -- special (but common) simple readonly cases
  if proxy[1] ~= nil then -- contains special readonly value
    local array = proxy
    proxy.read = function()return array_read(array) end
  end

  if proxy.open then
    return proxy.open(mode)
  end

  if bRead and not proxy.read then
    return nil, "cannot open for read"
  elseif bWrite and not proxy.write then
    return nil, "cannot open for write"
  end

  local txtRead = bRead and proxy.read()

  if bWrite then
    return text.internal.writer(proxy.write, mode, txtRead)
  end

  return text.internal.reader(txtRead, mode)
end

-- as long as the fsnode hack is used, fs.isLink is not needed here
-- function api.proxy.isLink(path) end

local function checked_invoke(handle, method, ...)
  checkArg(1, handle, "table")
  checkArg(2, method, "string")
  checkArg(3, handle[method], "function", "table", "nil")
  local m = handle[method]
  if not m then
    return nil, "bad file handle"
  elseif type(m) == "table" then
    local mm = getmetatable(m)
    assert(mm and mm.__call, string.format("FILE handle [%s] method defined, but is not callable", tostring(method)))
  end
  return m(handle, ...)
end

function api.proxy.read(h, ...)
  return checked_invoke(h, "read", ...)
end

function api.proxy.close(h, ...)
  return checked_invoke(h, "close", ...)
end

function api.proxy.write(h, ...)
  return checked_invoke(h, "write", ...)
end

function api.proxy.seek(h, ...)
  return checked_invoke(h, "seek", ...)
end

function api.proxy.remove()
  return nil, "cannot remove file or directory"
end

function api.proxy.makeDirectory()
  return nil, "use create in the devfs api"
end

function api.proxy.setLabel()
  return nil, "cannot set label on devfs"
end

return api
