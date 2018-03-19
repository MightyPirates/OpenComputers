local computer = require("computer")
local unicode = require("unicode")

local buffer = {}
local metatable = {
  __index = buffer,
  __metatable = "file"
}

function buffer.new(mode, stream)
  local result = {
    closed = false,
    tty = false,
    mode = {},
    stream = stream,
    bufferRead = "",
    bufferWrite = "",
    bufferSize = math.max(512, math.min(8 * 1024, computer.freeMemory() / 8)),
    bufferMode = "full",
    readTimeout = math.huge,
  }
  mode = mode or "r"
  for i = 1, unicode.len(mode) do
    result.mode[unicode.sub(mode, i, i)] = true
  end
  -- when stream closes, result should close first
  -- when result closes, stream should close after
  -- when stream closes, it is removed from the proc
  stream.close = setmetatable({close = stream.close,parent = result},{__call = buffer.close})
  return setmetatable(result, metatable)
end

function buffer:close()
  -- self is either the buffer, or the stream.close callable
  local meta = getmetatable(self)
  if meta == metatable.__metatable then
    return self.stream:close()
  end
  local parent = self.parent

  if parent.mode.w or parent.mode.a then
    parent:flush()
  end
  parent.closed = true
  return self.close(parent.stream)
end

function buffer:flush()
  if #self.bufferWrite > 0 then
    local tmp = self.bufferWrite
    self.bufferWrite = ""
    local result, reason = self.stream:write(tmp)
    if not result then
      return nil, reason or "bad file descriptor"
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

local function readChunk(self)
  if computer.uptime() > self.timeout then
    error("timeout")
  end
  local result, reason = self.stream:read(math.max(1,self.bufferSize))
  if result then
    self.bufferRead = self.bufferRead .. result
    return self
  else -- error or eof
    return result, reason
  end
end

function buffer:readLine(chop, timeout)
  self.timeout = timeout or (computer.uptime() + self.readTimeout)
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
      local result, reason = readChunk(self)
      if not result then
        if reason then
          return result, reason
        else -- eof
          result = #self.bufferRead > 0 and self.bufferRead or nil
          self.bufferRead = ""
          return result
        end
      end
    end
  end
end

function buffer:read(...)
  if not self.mode.r then
    return nil, "read mode was not enabled for this stream"
  end

  if self.mode.w or self.mode.a then
    self:flush()
  end

  if select("#", ...) == 0 then
    return self:readLine(true)
  end
  return self:formatted_read(readChunk, ...)
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

    if self.bufferMode == "no" then
      result, reason = self.stream:write(arg)
    else
      result, reason = self:buffered_write(arg)
    end

    if not result then
      return nil, reason
    end
  end

  return self
end

require("package").delay(buffer, "/lib/core/full_buffer.lua")

return buffer
