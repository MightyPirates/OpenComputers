local unicode = require("unicode")

--for k in pairs(buffer.reads) do print(k, #buffer.reads[k]) end
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
      local result, reason = readChunk()
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

return adv_buf
