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
  return (data:gsub('..', function (cc)
    return string.char(tonumber(cc, 16))
    end))
end

-- Applies base64 encoding.
function data.encode64(data)
  return component.data.encode64(data)
end

-- Applies base64 decoding.
function data.decode64(data)
  return component.data.decode64(data)
end

-- Returns raw/binary SHA2-256 hash of data. Common form of presenting SHA is hexadecimal string, see data.toHex.
function data.sha256(data)
  return component.data.sha256(data)
end

-- Returns raw/binary MD5 hash of data. Common form of presenting SHA is hexadecimal string, see data.toHex.
function data.md5(data)
  return component.data.md5(data)
end

-- Returns raw/binary CRC-32 hash of data. Common form of presenting SHA is hexadecimal string, see data.toHex.
function data.crc32(data)
  return component.data.crc32(data)
end


-- Applies DEFLATE compression.
function data.deflate(data)
  return component.data.deflate(data)
end

-- Applies INFLATE decompression.
function data.inflate(data)
  return component.data.inflate(data)
end

return data
