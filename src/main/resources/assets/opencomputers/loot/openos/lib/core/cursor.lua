local unicode = require("unicode")
local kb = require("keyboard")
local tty = require("tty")
local text = require("text")
local computer = require("computer")
local keys = kb.keys

local core_cursor = {}

core_cursor.vertical = {}

function core_cursor.vertical:move(n)
  local s = math.max(math.min(self.index + n, self.len), 0)
  if s == self.index then return end
  local echo_cmd = keys.left
  local from = s + 1
  local to = self.index
  if s > self.index then
    echo_cmd, from, to = keys.right, to + 1, s
  end
  self.index = s
  local step = unicode.wlen(unicode.sub(self.data, from, to))
  self:echo(echo_cmd, step)
end

-- back is used when arg comes after the cursor
function core_cursor.vertical:update(arg, back)
  if not arg then
    self.tails  = {}
    self.data   = ""
    self.index  = 0
    self.sy     = 0
    self.hindex = 0
  end
  local s1 = unicode.sub(self.data, 1, self.index)
  local s2 = unicode.sub(self.data, self.index + 1)
  if type(arg) == "string" then
    if back == false then
      arg, s2 = arg .. s2, ""
    else
      self.index = self.index + unicode.len(arg)
      self:echo(arg)
    end
    self.data = s1 .. arg
  elseif arg then -- number
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
  self:move(back or 0)
  if #s2 > 0 then
    self:update(s2, -unicode.len(s2))
  end
end

function core_cursor.vertical:echo(arg, num)
  local win = tty.window
  local gpu = win.gpu

  -- we should not use io.write
  -- the cursor should echo to the stream it is reading from
  -- this makes sense because a process may redirect its io
  -- but a cursor reading from a given stdin tty should also
  -- echo to that same stream
  -- but, if stdin has been piped - we do not echo the cursor
  if not io.stdin.tty then
    return
  end
  local out = io.stdin.stream

  if not gpu then return end
  win.nowrap = self.nowrap
  if arg == "" then -- special scroll request
    local width, x, y = win.width, win.x, win.y
    if x > width then
      win.x = ((x - 1) % width) + 1
      win.y = y + math.floor(x / width)
      out:write("") -- tty.stream:write knows how to scroll vertically
      x, y = win.x, win.y
    end
    if x <= 0 or y <= 0 or y > win.height or not gpu then return end
    return table.pack(select(2, pcall(gpu.get, x + win.dx, y + win.dy)))
  elseif arg == keys.left then
    local x = win.x - num
    local y = win.y
    while x < 1 do
      x = x + win.width - #(self.tails[win.dy + y - self.sy - 1] or "")
      y = y - 1
    end
    win.x, win.y = x, y
    arg = ""
  elseif arg == keys.right then
    local x = win.x + num
    local y = win.y
    while true do
      local width = win.width - #(self.tails[win.dy + y - self.sy] or "")
      if x <= width then break end
      x = x - width
      y = y + 1
    end
    win.x, win.y = x, y
    arg = ""
  elseif not arg or arg == true then -- blink
    local char = self.char_at_cursor
    if (arg == nil and not char) or (arg and not self.blinked) then
      char = char or self:echo("") --scroll and get char
      if not char[1] then return false end
      self.blinked = true
      if not arg then
        out:write("\0277")
        char.saved = win.saved
        gpu.setForeground(char[4] or char[2], not not char[4])
        gpu.setBackground(char[5] or char[3], not not char[5])
      end
      out:write("\0277\27[7m"..char[1].."\0278")
    elseif (arg and self.blinked) or (arg == false and char) then
      self.blinked = false
      gpu.set(win.x + win.dx, win.y + win.dy, char[1])
      if not arg then
        win.saved = char.saved
        out:write("\0278")
        char = nil
      end
    end
    self.char_at_cursor = char
    return true
  end
  return out:write(arg)
end

function core_cursor.vertical:handle(name, char, code)
  if name == "clipboard" then
    self.cache = nil -- this stops tab completion
    local newline = char:find("\10") or #char
    local printable_prefix, remainder = char:sub(1, newline), char:sub(newline + 1)
    self:update(printable_prefix)
    self:update(remainder, false)
  elseif name == "touch" or name == "drag" then
    core_cursor.touch(self, char, code)
  elseif name == "interrupted" then
    self:echo("^C\n")
    return false, name
  elseif name == "key_down" then
    local data = self.data
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
      self:update("\n")
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
      local value = ctrl and ((unicode.sub(data, 1, self.index):find("%s[^%s]+%s*$") or 0) - self.index) or -1
      if code == keys.left then
        self:move(value)
      else
        self:update(value)
      end
    elseif code == keys.right  then
      self:move(ctrl and ((data:find("%s[^%s]", self.index + 1) or self.len) - self.index) or 1)
    elseif code == keys.home   then self:move(-self.len)
    elseif code == keys["end"] then self:move( self.len)
    elseif code == keys.delete then self:update(1)
    elseif char >= 32          then self:update(unicode.char(char))
    else                            self.cache = backup_cache -- ignored chars shouldn't clear hint cache
    end
  end
  return true
end

-- echo'd to clear the input text in the tty
core_cursor.vertical.clear = "\27[J"

function core_cursor.new(base, index)
  -- if base has defined any methods, those are called first
  -- any new methods here are "super" methods to base
  base = base or {}
  base.super = base.super or index or core_cursor.vertical
  setmetatable(base, getmetatable(base) or { __index = base.super })
  if not base.data then
    base:update()
  end
  return base
end

function core_cursor.read(cursor)
  local last = cursor.next or ""
  cursor.next = nil
  if #last > 0 then
    cursor:handle("clipboard", last)
  end

  -- address checks
  local address_check =
  {
    key_down = tty.keyboard,
    clipboard = tty.keyboard,
    touch = tty.screen,
    drag = tty.screen,
    drop = tty.screen
  }

  while true do
    local next_line = cursor.data:find("\10")
    if next_line then
      local result = cursor.data:sub(1, next_line)
      local overflow = cursor.data:sub(next_line + 1)
      local history = text.trim(result)
      if history ~= "" and history ~= cursor[1] then
        table.insert(cursor, 1, history)
        cursor[(tonumber(os.getenv("HISTSIZE")) or 10) + 1] = nil
      end
      cursor[0] = nil
      cursor:update()
      cursor.next = overflow
      return result
    end

    cursor:echo()
    local pack = table.pack(computer.pullSignal(tty.window.blink and .5 or math.huge))
    local name = pack[1]
    cursor:echo(not name)

    if name then
      local filter_address = address_check[name]
      if not filter_address or filter_address() == pack[2] then
        local ret, why = cursor:handle(name, table.unpack(pack, 3, pack.n))
        if not ret then
          return ret, why
        end
      end
    end
  end
end

require("package").delay(core_cursor, "/lib/core/full_cursor.lua")

return core_cursor
