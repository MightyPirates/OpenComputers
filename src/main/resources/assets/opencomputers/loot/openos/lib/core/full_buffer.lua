local buffer = require("buffer")
local unicode = require("unicode")

function buffer:getTimeout()
  return self.readTimeout
end

function buffer:setTimeout(value)
  self.readTimeout = tonumber(value)
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

function buffer:buffered_write(arg)
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
  else--if self.bufferMode == "line" then
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
  end
  return result, reason
end

----------------------------------------------------------------------------------------------

function buffer:readNumber(readChunk)
  local len, sub
  if self.mode.b then
    len = rawlen
    sub = string.sub
  else
    len = unicode.len
    sub = unicode.sub
  end

  local number_text = ""
  local white_done

  local function peek()
    if len(self.bufferRead) == 0 then
      local result, reason = readChunk(self)
      if not result then
        return result, reason
      end
    end
    return sub(self.bufferRead, 1, 1)
  end

  local function pop()
    local n = sub(self.bufferRead, 1, 1)
    self.bufferRead = sub(self.bufferRead, 2)
    return n
  end

  while true do
    local peeked = peek()
    if not peeked then
      break
    end

    if peeked:match("[%s]") then
      if white_done then
        break
      end
      pop()
    else
      white_done = true
      if not tonumber(number_text .. peeked .. "0") then
        break
      end
      number_text = number_text .. pop() -- add pop to number_text
    end
  end

  return tonumber(number_text)
end

function buffer:readBytesOrChars(readChunk, n)
  n = math.max(n, 0)
  local len, sub
  if self.mode.b then
    len = rawlen
    sub = string.sub
  else
    len = unicode.len
    sub = unicode.sub
  end
  local data = ""
  while true do
    local current_data_len = len(data)
    local needed = n - current_data_len
    if needed < 1 then
      break
    end
    -- if the buffer is empty OR there is only 1 char left, read next chunk
    -- this is to protect that last byte from bad unicode
    if #self.bufferRead == 0 then
      local result, reason = readChunk(self)
      if not result then
        if reason then
          return result, reason
        else -- eof
          return current_data_len > 0 and data or nil
        end
      end
    end
    local splice = self.bufferRead
    if len(self.bufferRead) > needed then
      splice = sub(self.bufferRead, 1, needed)
      if len(splice) ~= needed then
        -- this can happen if the stream does not represent valid utf8 sequences
        -- we could search the string for the bad sequence but regardless, we're going to just return the raw data
        splice = self.bufferRead -- yes this is more than the user is asking for, but this is better than corrupting the stream
      end
    -- else -- we will read more chunks
    end
    data = data .. splice
    self.bufferRead = string.sub(self.bufferRead, #splice + 1)
  end
  return data
end

function buffer:readAll(readChunk)
  repeat
    local result, reason = readChunk(self)
    if not result and reason then
      return result, reason
    end
  until not result -- eof
  local result = self.bufferRead
  self.bufferRead = ""
  return result
end

function buffer:formatted_read(readChunk, ...)
  self.timeout = require("computer").uptime() + self.readTimeout
  local function read(n, format)
    if type(format) == "number" then
      return self:readBytesOrChars(readChunk, format)
    else
      local first_char_index = 1
      if type(format) ~= "string" then
        error("bad argument #" .. n .. " (invalid option)")
      elseif unicode.sub(format, 1, 1) == "*"  then
        first_char_index = 2
      end
      format = unicode.sub(format, first_char_index, first_char_index)
      if format == "n" then
        return self:readNumber(readChunk)
      elseif format == "l" then
        return self:readLine(true, self.timeout)
      elseif format == "L" then
        return self:readLine(false, self.timeout)
      elseif format == "a" then
        return self:readAll(readChunk)
      else
        error("bad argument #" .. n .. " (invalid format)")
      end
    end
  end

  local results = {}
  local formats = table.pack(...)
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

function buffer:size()
  local len = self.mode.b and rawlen or unicode.len
  local size = len(self.bufferRead)
  if self.stream.size then
    size = size + self.stream:size()
  end
  return size
end
