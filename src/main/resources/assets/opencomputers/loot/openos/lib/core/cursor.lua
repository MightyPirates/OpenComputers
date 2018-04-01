local unicode = require("unicode")
local kb = require("keyboard")
local keys = kb.keys

local core_cursor = {}

local super = {}
function super:move(n)
  local s =
    n < 0 and self.index > 0 and -1 or
    n > 0 and self.index < self.len and 1 or
    0
  if s == 0 then return end
  local echo_cmd = (s > 0 and keys.right or keys.left)
  self.index = self.index + s
  self:echo(echo_cmd)
  return self:move(n - s)
end

-- back is used when arg comes after the cursor
function super:update(arg, back)
  local s1 = unicode.sub(self.data, 1, self.index)
  local s2 = unicode.sub(self.data, self.index + 1)
  if type(arg) == "string" then
    self.data = s1 .. arg
    self.index = self.index + unicode.len(arg)
    self:echo(arg)
    self:move(back or 0)
  else -- number
    local has_tail = arg < 0 or #s2 > 0
    if arg < 0 then
      -- backspace? ignore if at start
      if self.index <= 0 then return end
      self:move(arg)
      s1 = unicode.sub(s1, 1, -1 + arg)
    else
      -- forward? ignore if at end
      if self.index >= self.len then return end
      s2 = unicode.sub(s2, 1 + arg)
    end
    self.data = s1
    if has_tail then
      self:echo(self.clear)
    end
  end
  self.len = unicode.len(self.data) -- recompute len
  if #s2 > 0 then
    self:update(s2, -unicode.len(s2))
  end
end

function super:echo(arg)
  if arg == "" then -- special scroll request
    local gpu, width, x, y =
      self.window.gpu,
      self.window.width,
      self.window.x,
      self.window.y
    if x > width then
      self.window.x = ((x - 1) % width) + 1
      self.window.y = y + math.floor(x / width)
      self.output:write("")
      x, y = self.window.x, self.window.y
    end
    return x > 0 and y > 0 and y <= self.window.height and gpu and
      select(2, pcall(gpu.get, x + self.window.dx, y + self.window.dy))
  elseif arg == keys.enter then
    arg = "\n"
  elseif arg == keys.left then
    local x = self.window.x - unicode.wlen(unicode.sub(self.data, self.index + 1, self.index + 1))
    local y = self.window.y
    if x < 1 then
      x = x + self.window.width - #(self.tails[self.window.dy + y - self.sy - 1] or "")
      y = y - 1
    end
    self.window.x, self.window.y = x, y
    arg = ""
  elseif arg == keys.right then
    local x = self.window.x + unicode.wlen(unicode.sub(self.data, self.index, self.index))
    local y = self.window.y
    local width = self.window.width - #(self.tails[self.window.dy + y - self.sy] or "")
    if x > width then
      x = x - width
      y = y + 1
    end
    self.window.x, self.window.y = x, y
    arg = ""
  elseif type(arg) == "boolean" or arg == nil then -- blink
    local char = self.char_at_cursor
    if (arg == nil and not char) or (arg and not self.blinked) then
      char = char or self:echo("") --scroll and get char
      if not char then return false end
      self.blinked = true
      arg = "\0277\27[7m" .. char .. "\0278"
    elseif (arg and self.blinked) or arg == false then
      self.blinked = false
      arg, char = "\0277" .. char .. "\0278", arg and char
    end
    self.char_at_cursor = char
  end
  return not arg or self.output:write(arg)
end

function super:handle(name, char, code)
  if name == "clipboard" then
    return core_cursor.clipboard(self, char, code)
  elseif name == "touch" or name == "drag" then
    return core_cursor.touch(self, char, code)
  elseif name == "interrupted" then
    self:echo("^C\n")
    return false, name
  elseif name ~= "key_down" then
    return true -- handled
  end

  local data = self.data
  local value = false
  local backup_cache = self.cache
  self.cache = nil
  local ctrl = kb.isControlDown()
  if ctrl and code == keys.d then
    return --nil:close
  elseif code == keys.tab then
    self.cache = backup_cache
    core_cursor.tab(self)
  elseif code == keys.enter or code == keys.numpadenter then
    self:move(self.len)
    self:echo(keys.enter)
    if data:find("%S") and data ~= self[1] then
      table.insert(self, 1, data)
      self[(tonumber(os.getenv("HISTSIZE")) or 10)+1] = nil
    end
    self[0] = nil
    return data .. "\n"
  elseif code == keys.up or code == keys.down then
    local ni = self.hindex + (code == keys.up and 1 or -1)
    if ni >= 0 and ni <= #self then
      self[self.hindex] = data
      self.hindex = ni
      self:move(self.len)
      self:update(-self.len)
      self:update(self[ni])
    end
  elseif code == keys.left or code == keys.back or code == keys.w and ctrl then
    value = ctrl and ((unicode.sub(data, 1, self.index):find("%s[^%s]+%s*$") or 0) - self.index) or -1
    if code == keys.left then
      self:move(value)
      value = false -- don't also update (cut)
    end
  elseif code == keys.right  then
    self:move(ctrl and ((data:find("%s[^%s]", self.index + 1) or self.len) - self.index) or 1)
  elseif code == keys.home   then self:move(-self.len)
  elseif code == keys["end"] then self:move( self.len)
  elseif code == keys.delete then value =  1
  elseif char >= 32          then value = unicode.char(char)
  else                            self.cache = backup_cache -- ignored chars shouldn't clear hint cache
  end
  if value then
    self:update(value)
  end
  return true
end

function core_cursor.new(base, window, output)
  local result = base or {}
  result.tails = {}
  result.data   = ""
  result.index  = 0
  result.len    = 0
  result.sy     = 0
  result.hindex = 0
  result.window = window
  result.output = output
  result.clear  = "\27[J" -- echo'd to clear the input text in the tty
  result.super  = super
  return setmetatable(result, { __index = super })
end

require("package").delay(core_cursor, "/lib/core/full_cursor.lua")

return core_cursor
