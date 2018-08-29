local bit32 = require("bit32")
local uuid = {}

function uuid.next()
  -- e.g. 3c44c8a9-0613-46a2-ad33-97b6ba2e9d9a
  -- 8-4-4-4-12 (halved sizes because bytes make hex pairs)
  local sets = {4, 2, 2, 2, 6}
  local result = ""
  local pos = 0

  for _,set in ipairs(sets) do
    if result:len() > 0 then
      result = result .. "-"
    end
    for _ = 1,set do
      local byte = math.random(0, 255)
      if pos == 6 then
        byte = bit32.bor(bit32.band(byte, 0x0F), 0x40)
      elseif pos == 8 then
        byte = bit32.bor(bit32.band(byte, 0x3F), 0x80)
      end
      result = result .. string.format("%02x", byte)
      pos = pos + 1
    end
  end

  return result
end

return uuid
