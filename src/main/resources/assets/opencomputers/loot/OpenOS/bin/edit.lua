local component = require("component")
local event = require("event")
local fs = require("filesystem")
local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

if not term.isAvailable() then
  return
end

local args, options = shell.parse(...)
if #args == 0 then
  io.write("Usage: edit <filename>")
  return
end

local filename = shell.resolve(args[1])

local readonly = options.r or fs.get(filename) == nil or fs.get(filename).isReadOnly()

if not fs.exists(filename) then
  if fs.isDirectory(filename) then
    io.stderr:write("file is a directory")
    return
  elseif readonly then
    io.stderr:write("file system is read only")
    return
  end
end

term.clear()
term.setCursorBlink(true)

local running = true
local buffer = {}
local scrollX, scrollY = 0, 0

-------------------------------------------------------------------------------

local function setStatus(value)
  local w, h = component.gpu.getResolution()
  component.gpu.set(1, h, text.padRight(unicode.sub(value, 1, w - 10), w - 10))
end

local function getSize()
  local w, h = component.gpu.getResolution()
  return w, h - 1
end

local function getCursor()
  local cx, cy = term.getCursor()
  return cx + scrollX, cy + scrollY
end

local function line()
  local cbx, cby = getCursor()
  return buffer[cby]
end

local function setCursor(nbx, nby)
  local w, h = getSize()
  nby = math.max(1, math.min(#buffer, nby))

  local ncy = nby - scrollY
  if ncy > h then
    term.setCursorBlink(false)
    local sy = nby - h
    local dy = math.abs(scrollY - sy)
    scrollY = sy
    component.gpu.copy(1, 1 + dy, w, h - dy, 0, -dy)
    for by = nby - (dy - 1), nby do
      local str = text.padRight(unicode.sub(buffer[by], 1 + scrollX), w)
      component.gpu.set(1, by - scrollY, str)
    end
  elseif ncy < 1 then
    term.setCursorBlink(false)
    local sy = nby - 1
    local dy = math.abs(scrollY - sy)
    scrollY = sy
    component.gpu.copy(1, 1, w, h - dy, 0, dy)
    for by = nby, nby + (dy - 1) do
      local str = text.padRight(unicode.sub(buffer[by], 1 + scrollX), w)
      component.gpu.set(1, by - scrollY, str)
    end
  end
  term.setCursor(term.getCursor(), nby - scrollY)

  nbx = math.max(1, math.min(unicode.len(line()) + 1, nbx))
  local ncx = nbx - scrollX
  if ncx > w then
    term.setCursorBlink(false)
    local sx = nbx - w
    local dx = math.abs(scrollX - sx)
    scrollX = sx
    component.gpu.copy(1 + dx, 1, w - dx, h, -dx, 0)
    for by = 1 + scrollY, math.min(h + scrollY, #buffer) do
      local str = unicode.sub(buffer[by], nbx - (dx - 1), nbx)
      str = text.padRight(str, dx)
      component.gpu.set(1 + (w - dx), by - scrollY, str)
    end
  elseif ncx < 1 then
    term.setCursorBlink(false)
    local sx = nbx - 1
    local dx = math.abs(scrollX - sx)
    scrollX = sx
    component.gpu.copy(1, 1, w - dx, h, dx, 0)
    for by = 1 + scrollY, math.min(h + scrollY, #buffer) do
      local str = unicode.sub(buffer[by], nbx, nbx + dx)
      --str = text.padRight(str, dx)
      component.gpu.set(1, by - scrollY, str)
    end
  end
  term.setCursor(nbx - scrollX, nby - scrollY)

  component.gpu.set(w - 9, h + 1, text.padLeft(string.format("%d,%d", nby, nbx), 10))
end

local function home()
  local cbx, cby = getCursor()
  setCursor(1, cby)
end

local function ende()
  local cbx, cby = getCursor()
  setCursor(unicode.len(line()) + 1, cby)
end

local function left()
  local cbx, cby = getCursor()
  if cbx > 1 then
    setCursor(cbx - 1, cby)
    return true -- for backspace
  elseif cby > 1 then
    setCursor(cbx, cby - 1)
    ende()
    return true -- again, for backspace
  end
end

local function right(n)
  n = n or 1
  local cbx, cby = getCursor()
  local be = unicode.len(line()) + 1
  if cbx < be then
    setCursor(cbx + n, cby)
  elseif cby < #buffer then
    setCursor(1, cby + 1)
  end
end

local function up(n)
  n = n or 1
  local cbx, cby = getCursor()
  if cby > 1 then
    setCursor(cbx, cby - n)
    if getCursor() > unicode.len(line()) then
      ende()
    end
  end
end

local function down(n)
  n = n or 1
  local cbx, cby = getCursor()
  if cby < #buffer then
    setCursor(cbx, cby + n)
    if getCursor() > unicode.len(line()) then
      ende()
    end
  end
end

local function delete()
  local cx, cy = term.getCursor()
  local cbx, cby = getCursor()
  local w, h = getSize()
  if cbx <= unicode.len(line()) then
    term.setCursorBlink(false)
    buffer[cby] = unicode.sub(line(), 1, cbx - 1) ..
                  unicode.sub(line(), cbx + 1)
    component.gpu.copy(cx + 1, cy, w - cx, 1, -1, 0)
    local br = cbx + (w - cx)
    local char = unicode.sub(line(), br, br)
    if not char or unicode.len(char) == 0 then
      char = " "
    end
    component.gpu.set(w, cy, char)
  elseif cby < #buffer then
    term.setCursorBlink(false)
    local append = table.remove(buffer, cby + 1)
    buffer[cby] = buffer[cby] .. append
    component.gpu.set(cx, cy, append)
    if cy < h then
      component.gpu.copy(1, cy + 2, w, h - (cy + 1), 0, -1)
      component.gpu.set(1, h, text.padRight(buffer[cby + (h - cy)], w))
    end
    setStatus("Save: [Ctrl+S] Close: [Ctrl+W]")
  end
end

local function insert(value)
  if not value or unicode.len(value) < 1 then
    return
  end
  term.setCursorBlink(false)
  local cx, cy = term.getCursor()
  local cbx, cby = getCursor()
  local w, h = getSize()
  buffer[cby] = unicode.sub(line(), 1, cbx - 1) ..
                value ..
                unicode.sub(line(), cbx)
  local len = unicode.len(value)
  local n = w - (cx - 1) - len
  if n > 0 then
    component.gpu.copy(cx, cy, n, 1, len, 0)
  end
  component.gpu.set(cx, cy, value)
  right(len)
  setStatus("Save: [Ctrl+S] Close: [Ctrl+W]")
end

local function enter()
  term.setCursorBlink(false)
  local cx, cy = term.getCursor()
  local cbx, cby = getCursor()
  local w, h = getSize()
  table.insert(buffer, cby + 1, unicode.sub(buffer[cby], cbx))
  buffer[cby] = unicode.sub(buffer[cby], 1, cbx - 1)
  component.gpu.fill(cx, cy, w - (cx - 1), 1, " ")
  if cy < h then
    if cy < h - 1 then
      component.gpu.copy(1, cy + 1, w, h - (cy + 1), 0, 1)
    end
    component.gpu.set(1, cy + 1, text.padRight(buffer[cby + 1], w))
  end
  setCursor(1, cby + 1)
  setStatus("Save: [Ctrl+S] Close: [Ctrl+W]")
end

local controlKeyCombos = {[keyboard.keys.s]=true,[keyboard.keys.w]=true,
                          [keyboard.keys.c]=true,[keyboard.keys.x]=true}
local function onKeyDown(char, code)
  if code == keyboard.keys.back and not readonly then
    if left() then
      delete()
    end
  elseif code == keyboard.keys.delete and not readonly then
    delete()
  elseif code == keyboard.keys.left then
    left()
  elseif code == keyboard.keys.right then
    right()
  elseif code == keyboard.keys.home then
    home()
  elseif code == keyboard.keys["end"] then
    ende()
  elseif code == keyboard.keys.up then
    up()
  elseif code == keyboard.keys.down then
    down()
  elseif code == keyboard.keys.pageUp then
    local w, h = getSize()
    up(h - 1)
  elseif code == keyboard.keys.pageDown then
    local w, h = getSize()
    down(h - 1)
  elseif code == keyboard.keys.enter and not readonly then
    enter()
  elseif keyboard.isControlDown() and controlKeyCombos[code] then
    local cbx, cby = getCursor()
    if code == keyboard.keys.s and not readonly then
      local new = not fs.exists(filename)
      local f, reason = io.open(filename, "w")
      if f then
        local chars, firstLine = 0, true
        for _, line in ipairs(buffer) do
          if not firstLine then
            line = "\n" .. line
          end
          firstLine = false
          f:write(line)
          chars = chars + unicode.len(line)
        end
        f:close()
        local format
        if new then
          format = [["%s" [New] %dL,%dC written]]
        else
          format = [["%s" %dL,%dC written]]
        end
        setStatus(string.format(format, fs.name(filename), #buffer, chars))
      else
        setStatus(reason)
      end
    elseif code == keyboard.keys.w or
           code == keyboard.keys.c or
           code == keyboard.keys.x
    then
      -- TODO ask to save if changed
      running = false
    end
  elseif readonly and code == keyboard.keys.q then
    running = false
  elseif not readonly then
    if not keyboard.isControl(char) then
      insert(unicode.char(char))
    elseif unicode.char(char) == "\t" then
      insert("  ")
    end
  end
end

local function onClipboard(value)
  local cbx, cby = getCursor()
  local start = 1
  local l = value:find("\n", 1, true)
  if l then
    repeat
      insert(string.sub(value, start, l - 1))
      enter()
      start = l + 1
      l = value:find("\n", start, true)
    until not l
  end
  insert(string.sub(value, start))
end

local function onClick(x, y)
  setCursor(x + scrollX, y + scrollY)
end

local function onScroll(direction)
  local cbx, cby = getCursor()
  setCursor(cbx, cby - direction * 12)
end

-------------------------------------------------------------------------------

do
  local f = io.open(filename)
  if f then
    local w, h = getSize()
    local chars = 0
    for line in f:lines() do
      if line:sub(-1) == "\r" then
        line = line:sub(1, -2)
      end
      table.insert(buffer, line)
      chars = chars + unicode.len(line)
      if #buffer <= h then
        component.gpu.set(1, #buffer, line)
      end
    end
    f:close()
    if #buffer == 0 then
      table.insert(buffer, "")
    end
    local format
    if readonly then
      format = [["%s" [readonly] %dL,%dC]]
    else
      format = [["%s" %dL,%dC]]
    end
    setStatus(string.format(format, fs.name(filename), #buffer, chars))
  else
    table.insert(buffer, "")
    setStatus(string.format([["%s" [New File] ]], fs.name(filename)))
  end
  setCursor(1, 1)
end

while running do
  local event, address, arg1, arg2, arg3 = event.pull()
  if type(address) == "string" and component.isPrimary(address) then
    local blink = true
    if event == "key_down" then
      onKeyDown(arg1, arg2)
    elseif event == "clipboard" and not readonly then
      onClipboard(arg1)
    elseif event == "touch" or event == "drag" then
      onClick(arg1, arg2)
    elseif event == "scroll" then
      onScroll(arg3)
    else
      blink = false
    end
    if blink then
      term.setCursorBlink(true)
      term.setCursorBlink(true) -- force toggle to caret
    end
  end
end

term.clear()
term.setCursorBlink(false)
