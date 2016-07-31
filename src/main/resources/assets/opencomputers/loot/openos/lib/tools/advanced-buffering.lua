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

return adv_buf
