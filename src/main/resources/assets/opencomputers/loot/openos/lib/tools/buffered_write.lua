local unicode = require("unicode")
local adv_buf = {}

function adv_buf.write(self, arg)
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
  else--if self.bufferMode == "line" then
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
  end
  return result, reason
end

return adv_buf
