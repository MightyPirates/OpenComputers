local buffer = require("buffer")
local component = require("component")
local event = require("event")

local internet = {}

-------------------------------------------------------------------------------

function internet.request(url, data)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")

  local inet = component.internet
  if not inet then
    error("no primary internet card found", 2)
  end

  local post
  if type(data) == "string" then
    post = data
  elseif type(data) == "table" then
    for k, v in pairs(data) do
      post = post and (post .. "&") or ""
      post = post .. tostring(k) .. "=" .. tostring(v)
    end
  end

  local result, reason = inet.request(url, post)
  if not result then
    error(reason, 2)
  end

  local handle = setmetatable({value=result}, {__gc=function(self)
    pcall(inet.close, self.value)
  end})

  return function()
    while true do
      local data, reason = inet.read(handle.value)
      if not data then
        inet.close(handle.value)
        if reason then
          error(reason, 2)
        else
          return nil -- eof
        end
      elseif #data > 0 then
        return data
      end
      -- else: no data, block
    end
  end
end

-------------------------------------------------------------------------------

local socketStream = {}

function socketStream:close()
  if self.handle then
    self.inet.close(self.handle)
    self.handle = nil
  end
end

function socketStream:seek()
  return nil, "bad file descriptor"
end

function socketStream:read(n)
  if not self.handle then
    return nil, "connection is closed"
  end
  return self.inet.read(self.handle, n)
end

function socketStream:write(value)
  if not self.handle then
    return nil, "connection is closed"
  end
  while #value > 0 do
    local written, reason = self.inet.write(self.handle, value)
    if not written then
      return nil, reason
    end
    value = string.sub(value, written + 1)
  end
  return true
end

function internet.socket(address, port)
  checkArg(1, address, "string")
  checkArg(2, port, "number", "nil")
  if port then
    address = address .. ":" .. port
  end

  local inet = component.internet
  local handle, reason = inet.connect(address)
  if not handle then
    return nil, reason
  end

  local stream = {inet = inet, handle = handle}

  -- stream:close does a syscall, which yields, and that's not possible in
  -- the __gc metamethod. So we start a timer to do the yield/cleanup.
  local function cleanup(self)
    if not self.handle then return end
    pcall(self.inet.close, self.handle)
  end
  local metatable = {__index = socketStream,
                     __gc = cleanup,
                     __metatable = "socketstream"}
  return setmetatable(stream, metatable)
end

function internet.open(address, port)
  local stream, reason = internet.socket(address, port)
  if not stream then
    return nil, reason
  end
  return buffer.new("rwb", stream)
end

-------------------------------------------------------------------------------

return internet