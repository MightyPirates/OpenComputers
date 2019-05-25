local buffer = require("buffer")
local component = require("component")
local event = require("event")

local internet = {}

-------------------------------------------------------------------------------

function internet.request(url, data, headers, method)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")
  checkArg(3, headers, "table", "nil")
  checkArg(4, method, "string", "nil")

  if not component.isAvailable("internet") then
    error("no primary internet card found", 2)
  end
  local inet = component.internet

  local post
  if type(data) == "string" then
    post = data
  elseif type(data) == "table" then
    for k, v in pairs(data) do
      post = post and (post .. "&") or ""
      post = post .. tostring(k) .. "=" .. tostring(v)
    end
  end

  local request, reason = inet.request(url, post, headers, method)
  if not request then
    error(reason, 2)
  end

  return setmetatable(
  {
    ["()"] = "function():string -- Tries to read data from the socket stream and return the read byte array.",
    close = setmetatable({},
    {
      __call = request.close,
      __tostring = function() return "function() -- closes the connection" end
    })
  },
  {
    __call = function()
      while true do
        local data, reason = request.read()
        if not data then
          request.close()
          if reason then
            error(reason, 2)
          else
            return nil -- eof
          end
        elseif #data > 0 then
          return data
        end
        -- else: no data, block
        os.sleep(0)
      end
    end,
    __index = request,
  })
end

-------------------------------------------------------------------------------

local socketStream = {}

function socketStream:close()
  if self.socket then
    self.socket.close()
    self.socket = nil
  end
end

function socketStream:seek()
  return nil, "bad file descriptor"
end

function socketStream:read(n)
  if not self.socket then
    return nil, "connection is closed"
  end
  return self.socket.read(n)
end

function socketStream:write(value)
  if not self.socket then
    return nil, "connection is closed"
  end
  while #value > 0 do
    local written, reason = self.socket.write(value)
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
  local socket, reason = inet.connect(address)
  if not socket then
    return nil, reason
  end

  local stream = {inet = inet, socket = socket}
  local metatable = {__index = socketStream,
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