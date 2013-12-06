local io, file = {}, {}

function file.new(mode, stream, nogc)
  local result = {
    mode = mode or "r",
    stream = stream,
    buffer = "",
    bufferSize = math.max(128, math.min(8 * 1024, computer.freeMemory() / 8)),
    bufferMode = "full"
  }
  local metatable = {
    __index = file,
    __metatable = "file"
  }
  return setmetatable(result, metatable)
end

function file:close()
  if self.mode ~= "r" and self.mode ~= "rb" then
    self:flush()
  end
  return self.stream:close()
end

function file:flush()
  local result, reason = self.stream:write(self.buffer)
  if result then
    self.buffer = ""
  else
    if reason then
      return nil, reason
    else
      return nil, "bad file descriptor"
    end
  end

  return self
end

function file:lines(...)
  local args = table.pack(...)
  return function()
    local result = table.pack(self:read(table.unpack(args, 1, args.n)))
    if not result[1] and result[2] then
      error(result[2])
    end
    return table.unpack(result, 1, result.n)
  end
end

function file:read(...)
  local function readChunk()
    local result, reason = self.stream:read(self.bufferSize)
    if result then
      self.buffer = self.buffer .. result
      return self
    else -- error or eof
      return nil, reason
    end
  end

  local function readBytesOrChars(n)
    n = math.max(n, 0)
    local len, sub
    if self.mode == "r" then
      len = unicode.len
      sub = unicode.sub
    else
      assert(self.mode == "rb")
      len = rawlen
      sub = string.sub
    end
    local buffer = ""
    repeat
      if len(self.buffer) == 0 then
        local result, reason = readChunk()
        if not result then
          if reason then
            return nil, reason
          else -- eof
            return #buffer > 0 and buffer or nil
          end
        end
      end
      local left = n - len(buffer)
      buffer = buffer .. sub(self.buffer, 1, left)
      self.buffer = sub(self.buffer, left + 1)
    until len(buffer) == n
    return buffer
  end

  local function readLine(chop)
    local start = 1
    while true do
      local l = self.buffer:find("\n", start, true)
      if l then
        local result = self.buffer:sub(1, l + (chop and -1 or 0))
        self.buffer = self.buffer:sub(l + 1)
        return result
      else
        start = #self.buffer
        local result, reason = readChunk()
        if not result then
          if reason then
            return nil, reason
          else -- eof
            local result = #self.buffer > 0 and self.buffer or nil
            self.buffer = ""
            return result
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
    until not result -- eof
    local result = self.buffer
    self.buffer = ""
    return result
  end

  local function read(n, format)
    if type(format) == "number" then
      return readBytesOrChars(format)
    else
      if type(format) ~= "string" or unicode.sub(format, 1, 1) ~= "*" then
        error("bad argument #" .. n .. " (invalid option)")
      end
      format = unicode.sub(format, 2, 2)
      if format == "n" then
        --[[ TODO ]]
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
  local formats = table.pack(...)
  if formats.n == 0 then
    return readLine(true)
  end
  for i = 1, formats.n do
    local result, reason = read(i, formats[i])
    if result then
      results[i] = result
    elseif reason then
      return nil, reason
    end
  end
  return table.unpack(results, 1, formats.n)
end

function file:seek(whence, offset)
  whence = tostring(whence or "cur")
  assert(whence == "set" or whence == "cur" or whence == "end",
    "bad argument #1 (set, cur or end expected, got " .. whence .. ")")
  offset = offset or 0
  checkArg(2, offset, "number")
  assert(math.floor(offset) == offset, "bad argument #2 (not an integer)")

  if whence == "cur" then
    offset = offset - #self.buffer
  end
  local result, reason = self.stream:seek(whence, offset)
  if result then
    self.buffer = ""
    return result
  else
    return nil, reason
  end
end

function file:setvbuf(mode, size)
  mode = mode or self.bufferMode
  size = size or self.bufferSize

  assert(mode == "no" or mode == "full" or mode == "line",
    "bad argument #1 (no, full or line expected, got " .. tostring(mode) .. ")")
  assert(mode == "no" or type(size) == "number",
    "bad argument #2 (number expected, got " .. type(size) .. ")")

  self.bufferMode = mode
  self.bufferSize = size

  return self.bufferMode, self.bufferSize
end

function file:write(...)
  local args = table.pack(...)
  for i = 1, args.n do
    if type(args[i]) == "number" then
      args[i] = tostring(args[i])
    end
    checkArg(i, args[i], "string")
  end

  for i = 1, args.n do
    local arg = args[i]
    local result, reason

    if self.bufferMode == "full" then
      if self.bufferSize - #self.buffer < #arg then
        result, reason = self:flush()
        if not result then
          return nil, reason
        end
      end
      if #arg > self.bufferSize then
        result, reason = self.stream:write(arg)
      else
        self.buffer = self.buffer .. arg
        result = self
      end

    elseif self.bufferMode == "line" then
      local l
      repeat
        local idx = arg:find("\n", (l or 0) + 1, true)
        if idx then
          l = idx
        end
      until not idx
      if l or #arg > self.bufferSize then
        result, reason = self:flush()
        if not result then
          return nil, reason
        end
      end
      if l then
        result, reason = self.stream:write(arg:sub(1, l))
        if not result then
          return nil, reason
        end
        arg = arg:sub(l + 1)
      end
      if #arg > self.bufferSize then
        result, reason = self.stream:write(arg)
      else
        self.buffer = self.buffer .. arg
        result = self
      end

    else -- self.bufferMode == "no"
      result, reason = self.stream:write(arg)
    end

    if not result then
      return nil, reason
    end
  end

  return self
end

-------------------------------------------------------------------------------

local stdinStream = {handle="stdin"}
local stdoutStream = {handle="stdout"}
local stdinHistory = {}

local function badFileDescriptor()
  return nil, "bad file descriptor"
end

function stdinStream:close()
  return nil, "cannot close standard file"
end
stdoutStream.close = stdinStream.close

function stdinStream:read(n)
  local result = term.read(stdinHistory)
  while #stdinHistory > 10 do
    table.remove(stdinHistory, 1)
  end
  return result
end

function stdoutStream:write(str)
  term.write(str, true)
  return self
end

stdinStream.seek = badFileDescriptor
stdinStream.write = badFileDescriptor
stdoutStream.read = badFileDescriptor
stdoutStream.seek = badFileDescriptor

-------------------------------------------------------------------------------

io.stdin = file.new("r", stdinStream, true)
io.stdout = file.new("w", stdoutStream, true)
io.stderr = io.stdout

io.stdout:setvbuf("no")

-------------------------------------------------------------------------------

local input, output = io.stdin, io.stdout

-------------------------------------------------------------------------------

function io.close(file)
  return (file or io.output()):close()
end

function io.flush()
  return io.output():flush()
end

function io.input(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file)
      if not result then
        error(reason)
      end
      input = result
    elseif io.type(file) then
      input = file
    else
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")")
    end
  end
  return input
end

function io.lines(filename, ...)
  if filename then
    local result, reason = io.open(filename)
    if not result then
      error(reason)
    end
    local args = table.pack(...)
    return function()
      local result = table.pack(file:read(table.unpack(args, 1, args.n)))
      if not result[1] then
        if result[2] then
          error(result[2])
        else -- eof
          file:close()
          return nil
        end
      end
      return table.unpack(result, 1, result.n)
    end
  else
    return io.input():lines()
  end
end

function io.open(path, mode)
  local stream, result = fs.open(path, mode)
  if stream then
    return file.new(mode, stream)
  else
    return nil, result
  end
end

function io.output(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file, "w")
      if not result then
        error(reason)
      end
      output = result
    elseif io.type(file) then
      output = file
    else
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")")
    end
  end
  return output
end

-- TODO io.popen = function(prog, mode) end

function io.read(...)
  return io.input():read(...)
end

function io.tmpfile()
  local name = os.tmpname()
  if name then
    return io.open(name, "a")
  end
end

function io.type(object)
  if type(object) == "table" then
    if getmetatable(object) == "file" then
      if object.stream.handle then
        return "file"
      else
        return "closed file"
      end
    end
  end
  return nil
end

function io.write(...)
  return io.output():write(...)
end

-------------------------------------------------------------------------------

_G.io = io
