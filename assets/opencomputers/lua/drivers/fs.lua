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

driver.fs = {}

function driver.fs.mount(fs, path)
  checkArg(1, fs, "string")
  local node = findNode(path, true)
  if node.fs then
    return nil, "another filesystem is already mounted here"
  end
  node.fs = fs
end

function driver.fs.umount(fsOrPath)
  local node, rest = findNode(fsOrPath)
  if not rest and node.fs then
    node.fs = nil
    removeEmptyNodes(node)
    return true
  else
    local queue = {mtab}
    repeat
      local node = table.remove(queue)
      if node.fs == fsOrPath then
        node.fs = nil
        removeEmptyNodes(node)
        return true
      end
      for _, child in ipairs(node.children) do
        table.insert(queue, child)
      end
    until #queue == 0
  end
end

-------------------------------------------------------------------------------

function driver.fs.exists(path)
  local node, rest = findNode(path)
  if not rest then -- virtual directory
    return true
  end
  if node.fs then
    return sendToNode(node.fs, "fs.exists", rest)
  end
end

function driver.fs.size(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return sendToNode(node.fs, "fs.size", rest)
  end
  return 0 -- no such file or directory or virtual directory
end

function driver.fs.listdir(path)
  local node, rest = findNode(path)
  local result
  if node.fs then
    result = {sendToNode(node.fs, "fs.list", rest or "")}
  else
    result = {}
  end
  if not rest then
    for k, _ in pairs(node.children) do
      table.insert(result, k .. "/")
    end
  end
  table.sort(result)
  return table.unpack(result)
end

-------------------------------------------------------------------------------

function driver.fs.remove(path)
  local node, rest = findNode(path)
  if node.fs and rest then
    return sendToNode(node.fs, "fs.remove", rest)
  end
end

function driver.fs.rename(oldPath, newPath)
  --[[ TODO moving between file systems will require actual data copying...
  local node, rest = findNode(path)
  local newNode, newRest = findNode(newPath)
  if node.fs and rest and newNode and newRest then
    return sendToNode(node.fs, "fs.rename", rest)
  end
  ]]
end

-------------------------------------------------------------------------------

local file = {}

function file.close(f)
  if f.handle then
    f:flush()
    sendToNode(f.fs, "fs.close", f.handle)
    f.handle = nil
  end
end

function file.flush(f)
  if not f.handle then
    return nil, "file is closed"
  end
  if #(f.buffer or "") > 0 then
    local result, reason = sendToNode(f.fs, "fs.write", f.buffer)
    if result then
      f.buffer = nil
    else
      if reason then
        return nil, reason
      else
        return nil, "invalid file"
      end
    end
  end
  return f
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

function file.seek(f, whence, offset)
  if not f.handle then
    return nil, "file is closed"
  end
  whence = whence or "cur"
  assert(whence == "set" or whence == "cur" or whence == "end",
    "bad argument #1 (set, cur or end expected, got " .. tostring(whence) .. ")")
  offset = offset or 0
  assert(type(offset) == "number",
    "bad argument #2 (number expected, got " .. type(offset) .. ")")
  assert(math.floor(offset) == offset,
    "bad argument #2 (not an integer)")

  if whence == "cur" and offset ~= 0 then
    offset = offset - #(f.buffer or "")
  end
  local result, reason = sendToNode(f.fs, "fs.seek", f.handle, whence, offset)
  if result then
    if offset ~= 0 then
      f.buffer = nil
    elseif whence == "cur" then
      result = result - #(f.buffer or "")
    end
  end
  return result, reason
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

function file.write(f, ...)
  if not f.handle then
    return nil, "file is closed"
  end
  local args = {...}
  for n, arg in ipairs(args) do
    if type(arg) == "number" then
      args[n] = tostring(arg)
    end
    checkArg(n, arg, "string")
  end
  for _, arg in ipairs(args) do
    --[[ TODO buffer
    if #buffer + #arg > bsize then
      flush()
    end
    buffer = buffer .. arg
    ]]
    sendToNode(f.fs, "fs.write", f.handle, arg)
  end
  return f
end

-------------------------------------------------------------------------------

function driver.fs.open(path, mode)
  mode = mode or "r"
  checkArg(2, mode, "string")
  assert(({r=true, rb=true, w=true, wb=true, a=true, ab=true})[mode],
    "bad argument #2 (r[b], w[b] or a[b] expected, got " .. tostring(mode) .. ")")
  local node, rest = findNode(path)
  if not node.fs or not rest then -- files can only be in file systems
    return nil, "file not found"
  end
  local handle, reason = sendToNode(node.fs, "fs.open", rest or "", mode)
  if not handle then
    return nil, reason
  end
  return setmetatable({
      fs = node.fs,
      handle = handle,
      bsize = 8 * 1024,
      bmode = "full"
    }, {
      __index = file,
      __gc = function(f)
        -- file.close does a syscall, which yields, and that's not possible in
        -- the __gc metamethod. So we start a timer to do the yield/cleanup.
        event.timer(0, function()
          file.close(f)
        end)
      end
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

-------------------------------------------------------------------------------

function loadfile(file, env)
  local f, reason = driver.fs.open(file)
  if not f then
    return nil, reason
  end
  local source, reason = f:read("*a")
  f:close()
  if not source then
    return nil, reason
  end
  return load(source, "=" .. file, env)
end

function dofile(file)
  local f, reason = loadfile(file)
  if not f then
    return nil, reason
  end
  return f()
end
