driver.fs = {}

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
      i = i + i
    end
  end
  return parts
end

local function findNode(path, create)
  local parts = segments(path)
  local node = mtab
  for _, part in ipairs(parts) do
    if not node.children[part] then
      if create then
        node.children[part] = {children={}}
      else
        return nil
      end
    end
    node = node.children[part]
  end
  return node
end

local function findFs(path)
  local parts = segments(path)
  local node = mtab
  for i = 1, #parts do
    if not node.children[parts[i]] then
      return node.fs, table.concat(parts, "/", i)
    end
    node = node.children[parts[i]]
  end
  return node.fs, ""
end
driver.fs.findFs = findFs

function driver.fs.mount(fs, path)
  assert(type(fs) == "string",
    "bad argument #1 (string expected, got " .. type(fs) .. ")")
  assert(type(path) == "string",
    "bad argument #2 (string expected, got " .. type(path) .. ")")
  local node = findNode(path, true)
  if node.fs then
    return nil, "another filesystem is already mounted here"
  end
  node.fs = fs
end

function driver.fs.umount(fsOrPath)
  assert(type(fsOrPath) == "string",
    "bad argument #1 (string expected, got " .. type(fsOrPath) .. ")")
  if type(fsOrPath) == "string" then
    local node = findNode(fsOrPath)
    if node and node.fs then
      node.fs = nil
      return true
    end
  else
    local queue = {mtab}
    repeat
      local node = table.remove(queue)
      if node.fs == fsOrPath then
        node.fs = nil
        return true
      end
      for _, child in ipairs(node.children) do
        table.insert(queue, child)
      end
    until #queue == 0
  end
end

function driver.fs.listdir(path)
  local fs, subpath = findFs(path)
  if fs then
    return sendToNode(fs, "fs.list", subpath)
  end
end

function driver.fs.remove(path)
end

function driver.fs.rename(oldPath, newPath)
end

function driver.fs.tmpname()
end

local file = {}

function file.close(f)
  if f.handle then
    f:flush()
    sendToNode(f.fs, "fs.close", f.handle)
    f.handle = nil
  end
end

function file.seek(f, whence, offset)
  if not f.handle then
    return nil, "file is closed"
  end
  whence = whence or "cur"
  assert(whence == "set" or whence == "cur" or whence == "end",
    "bad argument #1 (set, cur or end expected, got " .. tostring(whence) .. ")")
  assert(type(offset) == "number",
    "bad argument #2 (number expected, got " .. type(offset) .. ")")
  sendToNode(f.fs, "fs.seek", f.handle, whence, offset)
end

function file.read(f, ...)
  if not f.handle then
    return nil, "file is closed"
  end
  local function readChunk()
    local read, reason = sendToNode(f.fs, "fs.read", f.handle, f.bsize)
    if read then
      f.buffer = (f.buffer or "") .. read
      return true
    else
      return nil, reason
    end
  end
  local function readBytes(n)
    while #(f.buffer or "") < n do
      local result, reason = readChunk()
      if not result then
        if reason then
          return nil, reason
        end
        break
      end
    end
    local result
    if f.buffer then
      if #f.buffer > format then
        result = f.buffer:bsub(1, format)
        f.buffer = f.buffer:bsub(format + 1)
      else
        result = f.buffer
        f.buffer = nil
      end
    end
    return result
  end
  local function readLine(chop)
    while true do
      local l = (f.buffer or ""):find("\n", 1, true)
      if l then
        local rl = l + (chop and -1 or 0)
        local line = f.buffer:bsub(1, rl)
        f.buffer = f.buffer:bsub(l + 1)
        return line
      else
        local result, reason = readChunk()
        if not result then
          if reason then
            return nil, reason
          else
            local line = f.buffer
            f.buffer = nil
            return line
          end
        end
      end
    end
  end
  local function readAll()
    repeat
      local result, reason = readChunk()
      if not result and reason then
        return nil, reason
      end
    until not result
    local result = f.buffer or ""
    f.buffer = nil
    return result
  end
  local function read(n, format)
    if type(format) == "number" then
      return readBytes(format)
    else
      if not type(format) == "string" or format:sub(1, 1) ~= "*" then
        error("bad argument #" .. n .. " (invalid option)")
      end
      format = format:sub(2, 2)
      if format == "n" then
        error("not implemented")
      elseif format == "l" then
        return readLine(true)
      elseif format == "L" then
        return readLine(false)
      elseif format == "a" then
        return readAll()
      else
        error("bad argument #" .. n .. " (invalid format)")
      end
    end
  end
  local results = {}
  local formats = {...}
  if #formats == 0 then
    return readLine(true)
  end
  for n, format in ipairs(formats) do
    table.insert(results, read(n, format))
  end
  return table.unpack(results)
end

function file.flush(f)
  if not f.handle then
    return nil, "file is closed"
  end
  if #(f.buffer or "") > 0 then
    local result, reason = sendToNode(f.fs, "fs.write", f.buffer)
    f.buffer = nil
    if not result then
      if reason then
        return nil, reason
      else
        return nil, "invalid file"
      end
    end
  end
  return f
end

function file.write(f, ...)
  if not f.handle then
    return nil, "file is closed"
  end
  local args = {...}
  for n, arg in ipairs(args) do
    if type(arg) == "number" then
      args[n] = tostring(arg)
    end
    if type(arg) ~= "string" then
      error("bad argument #" .. n .. " (string or number expected, got " .. type(arg) .. ")")
    end
  end
  for _, arg in ipairs(args) do
    --[[ TODO buffer
    if #buffer + #arg > bsize then
      flush()
    end
    buffer = buffer .. arg
    ]]
    sendToNode(f.fs, "fs.write", arg)
  end
  return f
end

function file.setvbuf(f, mode, size)
  if not f.handle then
    return nil, "file is closed"
  end
  assert(mode == "no" or mode == "full" or mode == "line",
    "bad argument #1 (no, full or line expected, got " .. tostring(mode) .. ")")
  assert(mode == "no" or type(size) == "number",
    "bad argument #2 (number expected, got " .. type(size) .. ")")
  f:flush()
  f.bmode = mode
  f.bsize = size
end

function driver.fs.open(path, mode)
  assert(type(path) == "string",
    "bad argument #1 (string expected, got " .. type(path) .. ")")
  assert(({r=true,rb=true,w=true,wb=true,a=true,ab=true})[mode],
    "bad argument #2 (r[b], w[b] or a[b] expected, got " .. tostring(mode) .. ")")
  local fs, subpath = findFs(path)
  if not fs then
    return nil, "file not found"
  end
  local handle, reason = sendToNode(fs, "fs.open", subpath, mode)
  if not handle then
    return nil, reason
  end
  return setmetatable({
      fs = fs,
      handle = handle,
      bsize = 0,
      bmode = "no"
    }, {
      __index = file,
      __gc = file.close
    })
end

function driver.fs.type(f)
  local info = getFileInfo(f, true)
  if not info then
    return nil
  elseif not info.handle then
    return "closed file"
  else
    return "file"
  end
end

--[[
-- Aliases for vanilla Lua.
os.remove = driver.fs.remove
os.rename = driver.fs.rename
os.tmpname = driver.fs.tmpname

io = {}
io.flush = function() end -- does nothing
-- TODO io.lines = function(filename) end
io.open = driver.fs.open
-- TODO io.popen = function(prog, mode) end
io.read = driver.fs.read
-- TODO io.tmpfile = function() end
io.type = driver.fs.type
]]

driver.fs.mount(os.romAddress(), "/rom")