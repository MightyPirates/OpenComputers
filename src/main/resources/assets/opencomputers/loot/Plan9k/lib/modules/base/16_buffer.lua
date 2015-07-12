local computer = computer
local unicode = unicode

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


local function badFileDescriptor()
    return nil, "bad file descriptor"
end

function buffer.pipe()
    local inStream = {}
    local outStream = {}
    
    local isOpen = true
    
    local buf = ""
    
    function inStream:close()
        buf = nil
        return true
    end
    
    function outStream:close()
        isOpen = false
        return true
    end
    
    
    function outStream:write(str)
        local notify = #buf == 0
        buf = buf .. str
        if notify then
            local sig = {"pipe", inStream}
            kernel.modules.threading.eachThread(function(thread)
                if thread.currentHandler == "pipe" then
                    thread.eventQueue[#thread.eventQueue + 1] = sig
                end
            end)
        end
        return self
    end
    
    function inStream:read(n, dobreak)
        if #buf == 0 then
            while isOpen and coroutine.yield("pipe") ~= inStream and #buf < 1 do end
            if #buf == 0 and not isOpen then
                buf = nil
            end
        end
        --kernel.io.println("Pipe insert: "..tostring(buf))
        local result = buf
        buf = buf and ""
        return result
    end
        
    inStream.seek = badFileDescriptor
    inStream.write = badFileDescriptor
    outStream.read = badFileDescriptor
    outStream.seek = badFileDescriptor
    
    local _in = buffer.new("r", inStream)
    local out = buffer.new("w", outStream)
    
    _in.remaining = function()
        return buf and #buf or -1
    end
    
    out:setvbuf("no")
    
    return _in, out
end

function buffer:close()
  if self.mode.w or self.mode.a then
    self:flush()
  end
  self.closed = true
  return self.stream:close()
end

function buffer:flush()
  local result, reason = self.stream:write(self.bufferWrite)
  if result then
    self.bufferWrite = ""
  else
    if reason then
      return nil, reason
    else
      return nil, "bad file descriptor"
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
  local timeout = computer.uptime() + self.readTimeout

  local function readChunk()
    if computer.uptime() > timeout then
      error("timeout")
    end
    local result, reason = self.stream:read(self.bufferSize)
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
    
    --kernel.io.println("buffer read: "..tostring(buffer))
    return buffer
  end

  local function readNumber()
    local len, sub
    if self.mode.b then
      len = rawlen
      sub = string.sub
    else
      len = unicode.len
      sub = unicode.sub
    end
    local buffer = ""
    local first = true
    local decimal = false
    local last = false
    local hex = false
    local pat = "^[0-9]+"
    local minbuf = 3 -- "+0x" (sign + hexadecimal tag)
    -- this function is used to read trailing numbers (1e2, 0x1p2, etc)
    local function readnum(checksign)
      local _buffer = ""
      local sign = ""
      while true do
        if len(self.bufferRead) == 0 then
          local result, reason = readChunk()
          if not result then
            if reason then
              return nil, reason
            else -- eof
              return #_buffer > 0 and (sign .. _buffer) or nil
            end
          end
        end
        if checksign then
          local _sign = sub(self.bufferRead, 1, 1)
          if _sign == "+" or _sign == "-" then
            -- "eat" the sign (Rio Lua behaviour)
            sign = sub(self.bufferRead, 1, 1)
            self.bufferRead = sub(self.bufferRead, 2)
          end
          checksign = false
        else
          local x,y = string.find(self.bufferRead, pat)
          if not x then
            break
          else
            _buffer = _buffer .. sub(self.bufferRead, 1, y)
            self.bufferRead = sub(self.bufferRead, y + 1)
          end
        end
      end
      return #_buffer > 0 and (sign .. _buffer) or nil
    end
    while true do
      if len(self.bufferRead) == 0 or len(self.bufferRead) < minbuf then
        local result, reason = readChunk()
        if not result then
          if reason then
            return nil, reason
          else -- eof
            return #buffer > 0 and tonumber(buffer) or nil
          end
        end
      end
      -- these ifs are here so we run the buffer check above
      if first then
        local sign = sub(self.bufferRead, 1, 1)
        if sign == "+" or sign == "-" then
          -- "eat" the sign (Rio Lua behaviour)
          buffer = buffer .. sub(self.bufferRead, 1, 1)
          self.bufferRead = sub(self.bufferRead, 2)
        end
        local hextag = sub(self.bufferRead, 1, 2)
        if hextag == "0x" or hextag == "0X" then
          pat = "^[0-9A-Fa-f]+"
          -- "eat" the 0x, see https://gist.github.com/SoniEx2/570a363d81b743353151
          buffer = buffer .. sub(self.bufferRead, 1, 2)
          self.bufferRead = sub(self.bufferRead, 3)
          hex = true
        end
        minbuf = 0
        first = false
      elseif decimal then
        local sep = sub(self.bufferRead, 1, 1)
        if sep == "." then
          buffer = buffer .. sep
          self.bufferRead = sub(self.bufferRead, 2)
          local temp = readnum(false) -- no sign
          if temp then
            buffer = buffer .. temp
          end
        end
        if not tonumber(buffer) then break end
        decimal = false
        last = true
        minbuf = 1
      elseif last then
        local tag = sub(self.bufferRead, 1, 1)
        if hex and (tag == "p" or tag == "P") then
          local temp = sub(self.bufferRead, 1, 1)
          self.bufferRead = sub(self.bufferRead, 2)
          local temp2 = readnum(true) -- this eats the next sign if any
          if temp2 then
            buffer = buffer .. temp .. temp2
          end
        elseif tag == "e" or tag == "E" then
          local temp = sub(self.bufferRead, 1, 1)
          self.bufferRead = sub(self.bufferRead, 2)
          local temp2 = readnum(true) -- this eats the next sign if any
          if temp2 then
            buffer = buffer .. temp .. temp2
          end
        end
        break
      else
        local x,y = string.find(self.bufferRead, pat)
        if not x then
          minbuf = 1
          decimal = true
        else
          buffer = buffer .. sub(self.bufferRead, 1, y)
          self.bufferRead = sub(self.bufferRead, y + 1)
        end
      end
    end
    return tonumber(buffer)
  end

  local function readLine(chop)
    local start = 1
    while true do
      local l = self.bufferRead:find("\n", start, true)
      if l then
        local result = self.bufferRead:sub(1, l + (chop and -1 or 0))
        self.bufferRead = self.bufferRead:sub(l + 1)
        return result
      else
        start = #self.bufferRead
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
        return readNumber()
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

for k,v in pairs(buffer) do
    _G[k] = v
end

