local component = require("component")


local data = {}
-------------------------------------------------------------------------------
-- Converts binary data into hexadecimal string.
function data.toHex(data)
  return (data:gsub('.', function (c)
    return string.format('%02X', string.byte(c))
    end))
end

-- Converts hexadecimal string into binary data.
function data.fromHex(hex)
  return (hex:gsub('..', function (cc)
    return string.char(tonumber(cc, 16))
    end))
end

if component.isAvailable("data") then
  local wrappedFunctions = { 'encode64', 'decode64', 'sha256', 'md5', 'crc32', 'deflate', 'inflate',
                             'getLimit', 'tier', 'encrypt', 'decrypt', 'random', 'generateKeyPair',
                             'deserializeKey', 'ecdh', 'ecdsa' }

  function data.present()
    return true
  end

  for _, v in ipairs(wrappedFunctions) do
    data[v] = component.data[v]
  end
else
  function data.present()
    return false
  end
end

return data
