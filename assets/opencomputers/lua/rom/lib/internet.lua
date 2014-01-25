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

  function pullResponse()
    while true do
      local _, responseUrl, result, reason = event.pull("http_response")
      if responseUrl == url then
        return result, reason
      end
    end
  end

  -- Wait for the first response, which tells us whether it was a success.
  local result, reason = pullResponse()
  if not result and reason then
    error(reason, 2)
  end
  return function()
    local thisResult
    if result then
      thisResult = result
      result = pullResponse()
    end
    return thisResult
  end
end

-------------------------------------------------------------------------------

local socketStream = {}

function socketStream.close(self)
  if self.handle then
    self.inet.close(self.handle)
    self.handle = nil
  end
end

function socketStream.seek()
  return nil, "bad file descriptor"
end

function socketStream.read(self, n)
  if not self.handle then
    return nil, "connection is closed"
  end
  local value = ""
  while n > #value do
    local read = self.inet.read(self.handle, n)
    if read then
      value = value .. read
    else
      break
    end
  end
  return value
end

function socketStream.write(self, value)
  if not self.handle then
    return nil, "connection is closed"
  end
  while #value > 0 do
    local written = self.inet.write(self.handle, value)
    value = string.sub(value, written + 1)
  end
end

function internet.socket(address, port)
  checkArg(1, address, "string")
  checkArg(2, port, "number", "nil")
  if port then
    address = address .. ":" .. port
  end

  local inet = component.internet
  local handle, reason = inet.open(address)
  if not handle then
    return nil, reason
  end

  local stream = {inet = inet, handle = handle}

  -- stream:close does a syscall, which yields, and that's not possible in
  -- the __gc metamethod. So we start a timer to do the yield/cleanup.
  local function cleanup(self)
    if not self.handle then return end
    -- save non-gc'ed values as upvalues
    local inet = self.inet
    local handle = self.handle
    local function close()
      inet.close(handle)
    end
    event.timer(0, close)
  end
  local metatable = {__index = socketStream,
                     __gc = cleanup,
                     __metatable = "socketstream"}
  return setmetatable(stream, metatable)
end

function internet.open(address, port)
  return buffer.new("rwb", internet.socket(address, port))
end

-------------------------------------------------------------------------------

return internet