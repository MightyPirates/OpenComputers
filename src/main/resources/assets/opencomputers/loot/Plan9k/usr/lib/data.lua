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

-- Forward everything else to the primary data card.
setmetatable(data, { __index = function(_, key) return component.data[key] end })

-------------------------------------------------------------------------------

return data
