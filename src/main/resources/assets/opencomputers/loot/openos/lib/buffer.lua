local computer = require("computer")
local unicode = require("unicode")

local buffer = {}

function buffer.new(mode, stream)
  local result = {
    mode = {},
    stream = stream,
    bufferRead = "",
    bufferWrite = "",
    bufferSize = math.max(512, math.min(8 * 1024, computer.freeMemory() / 8)),
    bufferMode = "full",
    readTimeout = math.huge
  }
  mode = mode or "r"
  for i = 1, unicode.len(mode) do
    result.mode[unicode.sub(mode, i, i)] = true
  end
  local metatable = {
    __index = buffer,
    __metatable = "file"
  }
  return setmetatable(result, metatable)
end

function buffer:close()
  if self.mode.w or self.mode.a then
    self:flush()
  end
  self.closed = true
  return self.stream:close()
end

function buffer:flush()
  if #self.bufferWrite > 0 then
    local tmp = self.bufferWrite
    self.bufferWrite = ""
    local result, reason = self.stream:write(tmp)
    if result then
      self.bufferWrite = ""
    else
      if reason then
        return nil, reason
      else
        return nil, "bad file descriptor"
      end
    end
  end

  return self
end

function buffer:lines(...)
  local args = table.pack(...)
  return function()
    local result = table.pack(self:read(table.unpack(args, 1, args.n)))
    if not result[1] and result[2] then
      error(result[2])
    end
    return table.unpack(result, 1, result.n)
  end
end

function buffer:read(...)
  if not self.mode.r then
    return nil, "read mode was not enabled for this stream"
  end

  local timeout = computer.uptime() + self.readTimeout

  local function readChunk()
    if computer.uptime() > timeout then
      error("timeout")
    end
    local result, reason = self.stream:read(math.max(1,self.bufferSize))
    if result then
      self.bufferRead = self.bufferRead .. result
      return self
    else -- error or eof
      return nil, reason
    end
  end

  local function readBytesOrChars(n)
    n = math.max(n, 0)
    local len, sub
    if self.mode.b then
      len = rawlen
      sub = string.sub
    else
      len = unicode.len
      sub = unicode.sub
    end
    local buffer = ""
    repeat
      if len(self.bufferRead) == 0 then
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
      buffer = buffer .. sub(self.bufferRead, 1, left)
      self.bufferRead = sub(self.bufferRead, left + 1)
    until len(buffer) == n
    return buffer
  end

  local function readLine(chop)
    local start = 1
    while true do
      local buf = self.bufferRead
      local i = buf:find("[\r\n]", start)
      local c = i and buf:sub(i,i)
      local is_cr = c == "\r"
      if i and (not is_cr or i < #buf) then
        local n = buf:sub(i+1,i+1)
        if is_cr and n == "\n" then
          c = c .. n
        end
        local result = buf:sub(1, i - 1) .. (chop and "" or c)
        self.bufferRead = buf:sub(i + #c)
        return result
      else
        start = #self.bufferRead - (is_cr and 1 or 0)
        local result, reason = readChunk()
        if not result then
          if reason then
            return nil, reason
          else -- eof
            local result = #self.bufferRead > 0 and self.bufferRead or nil
            self.bufferRead = ""
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
    local result = self.bufferRead
    self.bufferRead = ""
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
        return require("tools/advanced-buffering").readNumber(self, readChunk)
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

  if self.mode.w or self.mode.a then
    self:flush()
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

function buffer:seek(whence, offset)
  whence = tostring(whence or "cur")
  assert(whence == "set" or whence == "cur" or whence == "end",
    "bad argument #1 (set, cur or end expected, got " .. whence .. ")")
  offset = offset or 0
  checkArg(2, offset, "number")
  assert(math.floor(offset) == offset, "bad argument #2 (not an integer)")

  if self.mode.w or self.mode.a then
    self:flush()
  elseif whence == "cur" then
    offset = offset - #self.bufferRead
  end
  local result, reason = self.stream:seek(whence, offset)
  if result then
    self.bufferRead = ""
    return result
  else
    return nil, reason
  end
end

function buffer:setvbuf(mode, size)
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

function buffer:getTimeout()
  return self.readTimeout
end

function buffer:setTimeout(value)
  self.readTimeout = tonumber(value)
end

function buffer:write(...)
  if self.closed then
    return nil, "bad file descriptor"
  end
  if not self.mode.w and not self.mode.a then
    return nil, "write mode was not enabled for this stream"
  end
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
      if self.bufferSize - #self.bufferWrite < #arg then
        result, reason = self:flush()
        if not result then
          return nil, reason
        end
      end
      if #arg > self.bufferSize then
        result, reason = self.stream:write(arg)
      else
        self.bufferWrite = self.bufferWrite .. arg
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
        self.bufferWrite = self.bufferWrite .. arg
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

return buffer
