local core_cursor = require("core/cursor")
local unicode = require("unicode")
local kb = require("keyboard")
local tty = require("tty")

core_cursor.horizontal = {}

function core_cursor.touch(cursor, gx, gy)
  if cursor.len > 0 then
    local win = tty.window
    gx, gy = gx - win.dx, gy - win.dy
    while true do
      local x, y, d = win.x, win.y, win.width
      local dx = ((gy*d+gx)-(y*d+x))
      if dx == 1 then
        dx = unicode.wlen(unicode.sub(cursor.data, cursor.index + 1, cursor.index + 1)) == 2 and 0 or dx
      end
      if dx == 0 then
        break
      end
      cursor:move(dx > 0 and 1 or -1)
      if x == win.x and y == win.y then
        break
      end
    end
  end
end

function core_cursor.tab(cursor)
  local hints = cursor.hint
  if not hints then return end
  if not cursor.cache then
    cursor.cache =
      type(hints) == "table" and hints or
      hints(cursor.data, cursor.index + 1) or
      {}
    cursor.cache.i = -1
  end

  local cache = cursor.cache
  
  if #cache == 1 and cache.i == 0 then
    -- there was only one solution, and the user is asking for the next
    cursor.cache = hints(cache[1], cursor.index + 1)
    if not cursor.cache then return end
    cursor.cache.i = -1
    cache = cursor.cache
  end

  local change = kb.isShiftDown() and -1 or 1
  cache.i = (cache.i + change) % math.max(#cache, 1)
  local next = cache[cache.i + 1]
  if next then
    local tail = unicode.len(cursor.data) - cursor.index
    cursor:move(cursor.len)
    cursor:update(-cursor.len)
    cursor:update(next, -tail)
  end
end

function core_cursor.horizontal:scroll(num, final_index)
  self:move(self.vindex - self.index) -- go to left edge
  -- shift (v)index by num
  self.vindex = self.vindex + num
  self.index = self.index + num

  self:echo("\0277".. -- remember the location
           unicode.sub(self.data, self.index + 1).. -- write part after
           "\27[K\0278") -- clear tail and restore left edge

  self:move(final_index - self.index) -- move to final_index location
end

function core_cursor.horizontal:echo(arg, num)
  local w = tty.window
  w.nowrap = self.nowrap
  if arg == "" then -- special scroll request
    local width = w.width
    if w.x >= width then
      -- the width that matters depends on the following char width
      width = width - math.max(unicode.wlen(unicode.sub(self.data, self.index + 1, self.index + 1)) - 1, 0)
      if w.x > width then
        local s1 = unicode.sub(self.data, self.vindex + 1, self.index)
        self:scroll(unicode.len(unicode.wtrunc(s1, w.x - width + 1)), self.index)
      end
    end
    -- scroll is safe now, return as normal below
  elseif arg == kb.keys.left then
    if self.index < self.vindex then
      local s2 = unicode.sub(self.data, self.index + 1)
      w.x = w.x - num + unicode.wlen(unicode.sub(s2, 1, self.vindex - self.index))
      local current_x = w.x
      self:echo(s2)
      w.x = current_x
      self.vindex = self.index
      return true
    end
  elseif arg == kb.keys.right then
    w.x = w.x + num
    return self:echo("") -- scroll
  end
  return core_cursor.vertical.echo(self, arg, num)
end

function core_cursor.horizontal:update(arg, back)
  if back then
    -- if we're just going to render arg and move back, and we're not wrapping, just render arg
    -- back may be more or less from current x
    self:update(arg, false)
    local x = tty.window.x
    self:echo(arg) -- nowrap echo
    tty.window.x = x
    self:move(self.len - self.index + back) -- back is negative from end
    return true
  elseif not arg then -- reset
    self.nowrap = true
    self.clear = "\27[K"
    self.vindex = 0 -- visual/virtual index
  end
  return core_cursor.vertical.update(self, arg, back)
end

setmetatable(core_cursor.horizontal, { __index = core_cursor.vertical })
