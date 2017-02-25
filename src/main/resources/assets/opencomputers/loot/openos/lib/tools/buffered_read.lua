local unicode = require("unicode")
local adv_buf = {}

function adv_buf.readNumber(self, readChunk)
  local len, sub
  if self.mode.b then
    len = rawlen
    sub = string.sub
  else
    len = unicode.len
    sub = unicode.sub
  end

  local buffer = ""
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

  local function take()
    buffer = buffer .. pop()
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
      if not tonumber(buffer .. peeked .. "0") then
        break
      end
      take() -- add pop to buffer
    end
  end

  return tonumber(buffer)
end

function adv_buf.readBytesOrChars(self, readChunk, n)
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
      local result, reason = readChunk(self)
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

function adv_buf.readAll(self, readChunk)
  repeat
    local result, reason = readChunk(self)
    if not result and reason then
      return nil, reason
    end
  until not result -- eof
  local result = self.bufferRead
  self.bufferRead = ""
  return result
end

function adv_buf.read(self, readChunk, formats)
  self.timeout = require("computer").uptime() + self.readTimeout
  local function read(n, format)
    if type(format) == "number" then
      return adv_buf.readBytesOrChars(self, readChunk, format)
    else
      local first_char_index = 1
      if type(format) ~= "string" then
        error("bad argument #" .. n .. " (invalid option)")
      elseif unicode.sub(format, 1, 1) == "*"  then
        first_char_index = 2
      end
      format = unicode.sub(format, first_char_index, first_char_index)
      if format == "n" then
        return adv_buf.readNumber(self, readChunk)
      elseif format == "l" then
        return self:readLine(true, self.timeout)
      elseif format == "L" then
        return self:readLine(false, self.timeout)
      elseif format == "a" then
        return adv_buf.readAll(self, readChunk)
      else
        error("bad argument #" .. n .. " (invalid format)")
      end
    end
  end

  local results = {}
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

function adv_buf.seek(self, whence, offset)
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

return adv_buf
