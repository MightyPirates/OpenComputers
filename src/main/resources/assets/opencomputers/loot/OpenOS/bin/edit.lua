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

local function loadConfig()
  -- Try to load user settings.
  local env = {}
  local config = loadfile("/etc/edit.cfg", nil, env)
  if config then
    pcall(config)
  end
  -- Fill in defaults.
  env.keybinds = env.keybinds or {
    left = {{"left"}},
    right = {{"right"}},
    up = {{"up"}},
    down = {{"down"}},
    home = {{"home"}},
    eol = {{"end"}},
    pageUp = {{"pageUp"}},
    pageDown = {{"pageDown"}},

    backspace = {{"back"}},
    delete = {{"delete"}},
    deleteLine = {{"control", "delete"}, {"shift", "delete"}},
    newline = {{"enter"}},

    save = {{"control", "s"}},
    close = {{"control", "w"}},
    find = {{"control", "f"}},
    findnext = {{"control", "g"}, {"control", "n"}, {"f3"}}
  }
  -- Generate config file if it didn't exist.
  if not config then
    local root = fs.get("/")
    if root and not root.isReadOnly() then
      fs.makeDirectory("/etc")
      local f = io.open("/etc/edit.cfg", "w")
      if f then
        local serialization = require("serialization")
        for k, v in pairs(env) do
          f:write(k.."="..tostring(serialization.serialize(v, math.huge)).."\n")
        end
        f:close()
      end
    end
  end
  return env
end

term.clear()
term.setCursorBlink(true)

local running = true
local buffer = {}
local scrollX, scrollY = 0, 0
local config = loadConfig()

local getKeyBindHandler -- forward declaration for refind()

local function helpStatusText()
  local function prettifyKeybind(label, command)
    local keybind = type(config.keybinds) == "table" and config.keybinds[command]
    if type(keybind) ~= "table" or type(keybind[1]) ~= "table" then return "" end
    local alt, control, shift, key
    for _, value in ipairs(keybind[1]) do
      if value == "alt" then alt = true
      elseif value == "control" then control = true
      elseif value == "shift" then shift = true
      else key = value end
    end
    if not key then return "" end
    return label .. ": [" ..
           (control and "Ctrl+" or "") ..
           (alt and "Alt+" or "") ..
           (shift and "Shift+" or "") ..
           unicode.upper(key) ..
           "] "
  end
  return prettifyKeybind("Save", "save") ..
         prettifyKeybind("Close", "close") ..
         prettifyKeybind("Find", "find")
end

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

local function highlight(bx, by, length, enabled)
  local w, h = getSize()
  local cx, cy = bx - scrollX, by - scrollY
  cx = math.max(1, math.min(w, cx))
  cy = math.max(1, math.min(h, cy))
  length = math.max(1, math.min(w - cx, length))

  local fg, fgp = component.gpu.getForeground()
  local bg, bgp = component.gpu.getBackground()
  if enabled then
    component.gpu.setForeground(bg, bgp)
    component.gpu.setBackground(fg, fgp)
  end
  local value = ""
  for x = cx, cx + length - 1 do
    value = value .. component.gpu.get(x, cy)
  end
  component.gpu.set(cx, cy, value)
  if enabled then
    component.gpu.setForeground(fg, fgp)
    component.gpu.setBackground(bg, bgp)
  end
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

local function delete(fullRow)
  local cx, cy = term.getCursor()
  local cbx, cby = getCursor()
  local w, h = getSize()
  local function deleteRow(row)
    local content = table.remove(buffer, row)
    local rcy = cy + (row - cby)
    if rcy <= h then
      component.gpu.copy(1, rcy + 1, w, h - rcy, 0, -1)
      component.gpu.set(1, h, text.padRight(buffer[row + (h - rcy)], w))
    end
    return content
  end
  if fullRow then
    term.setCursorBlink(false)
    if #buffer > 1 then
      deleteRow(cby)
    else
      buffer[cby] = ""
      component.gpu.fill(1, cy, w, 1, " ")
    end
    setCursor(1, cby)
  elseif cbx <= unicode.len(line()) then
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
    local append = deleteRow(cby + 1)
    buffer[cby] = buffer[cby] .. append
    component.gpu.set(cx, cy, append)
  else
    return
  end
  setStatus(helpStatusText())
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
  setStatus(helpStatusText())
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
  setStatus(helpStatusText())
end

local findText = ""

local function find()
  local w, h = getSize()
  local cx, cy = term.getCursor()
  local cbx, cby = getCursor()
  local ibx, iby = cbx, cby
  while running do
    if unicode.len(findText) > 0 then
      local sx, sy
      for syo = 1, #buffer do -- iterate lines with wraparound
        sy = (iby + syo - 1 + #buffer - 1) % #buffer + 1
        sx = string.find(buffer[sy], findText, syo == 1 and ibx or 1, true)
        if sx and (sx >= ibx or syo > 1) then
          break
        end
      end
      if not sx then -- special case for single matches
        sy = iby
        sx = string.find(buffer[sy], findText, nil, true)
      end
      if sx then
        cbx, cby = sx, sy
        setCursor(cbx, cby)
        highlight(cbx, cby, unicode.len(findText), true)
      end
    end
    term.setCursor(7 + unicode.len(findText), h + 1)
    setStatus("Find: " .. findText)

    local _, _, char, code = event.pull("key_down")
    local handler, name = getKeyBindHandler(code)
    highlight(cbx, cby, unicode.len(findText), false)
    if name == "newline" then
      break
    elseif name == "close" then
      handler()
    elseif name == "backspace" then
      findText = unicode.sub(findText, 1, -2)
    elseif name == "find" or name == "findnext" then
      ibx = cbx + 1
      iby = cby
    elseif not keyboard.isControl(char) then
      findText = findText .. unicode.char(char)
    end
  end
  setCursor(cbx, cby)
  setStatus(helpStatusText())
end

-------------------------------------------------------------------------------

local keyBindHandlers = {
  left = left,
  right = right,
  up = up,
  down = down,
  home = home,
  eol = ende,
  pageUp = function()
    local w, h = getSize()
    up(h - 1)
  end,
  pageDown = function()
    local w, h = getSize()
    down(h - 1)
  end,

  backspace = function()
    if not readonly and left() then
      delete()
    end
  end,
  delete = function()
    if not readonly then
      delete()
    end
  end,
  deleteLine = function()
    if not readonly then
      delete(true)
    end
  end,
  newline = function()
    if not readonly then
      enter()
    end
  end,

  save = function()
    if readonly then return end
    local new = not fs.exists(filename)
    local backup
    if not new then
      backup = filename .. "~"
      for i = 1, math.huge do
        if not fs.exists(backup) then
          break
        end
        backup = filename .. "~" .. i
      end
      fs.copy(filename, backup)
    end
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
    if not new then
      fs.remove(backup)
    end
  end,
  close = function()
    -- TODO ask to save if changed
    running = false
  end,
  find = function()
    findText = ""
    find()
  end,
  findnext = find
}

getKeyBindHandler = function(code)
  if type(config.keybinds) ~= "table" then return end
  -- Look for matches, prefer more 'precise' keybinds, e.g. prefer
  -- ctrl+del over del.
  local result, resultName, resultWeight = nil, nil, 0
  for command, keybinds in pairs(config.keybinds) do
    if type(keybinds) == "table" and keyBindHandlers[command] then
      for _, keybind in ipairs(keybinds) do
        if type(keybind) == "table" then
          local alt, control, shift, key
          for _, value in ipairs(keybind) do
            if value == "alt" then alt = true
            elseif value == "control" then control = true
            elseif value == "shift" then shift = true
            else key = value end
          end
          if (not alt or keyboard.isAltDown()) and
             (not control or keyboard.isControlDown()) and
             (not shift or keyboard.isShiftDown()) and
             code == keyboard.keys[key] and
             #keybind > resultWeight
          then
            resultWeight = #keybind
            resultName = command
            result = keyBindHandlers[command]
          end
        end
      end
    end
  end
  return result, resultName
end

-------------------------------------------------------------------------------

local function onKeyDown(char, code)
  local handler = getKeyBindHandler(code)
  if handler then
    handler()
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
  value = value:gsub("\r\n", "\n")
  local cbx, cby = getCursor()
  local start = 1
  local l = value:find("\n", 1, true)
  if l then
    repeat
      local line = string.sub(value, start, l - 1)
      line = text.detab(line, 2)
      insert(line)
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
