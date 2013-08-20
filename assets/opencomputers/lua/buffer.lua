--[[
  This is the terminal implementation.

  It provides means to interact with a screen that is provided by the actual
  game in some way.
]]

function write(term, ...)
  local w, h = term.getSize()
  local x, y = term.getCursorPosition()
  local foreground = term.getForeground()
  local background = term.getBackground()
  for _, value in ipairs({...}) do
    local value = tostring(value)
    for i in 1, value:len() do
      term.setCharAt(x, y) = value.sub(i, i)
    end
  end
end

--[[ Creates a new terminal with an internal display buffer. ]]
function new(width, height)
  -- This is the current display buffer of the terminal: an array of lines, which
  -- are in turn arrays of the displayed characters on that line, as well as
  -- foreground and background colors (which may or may not be ignored in the
  -- actual presentation layer).
  local buffer = {}

  -- This is the size of the buffer, i.e. its width and height.
  local size = {x = 0, y = 0}

  -- Cursor position and whether to blink or not.
  local cursor = {x = 1, y = 1, blink = false}

  -- The current foreground and background color, applied when writing.
  local colors = {foreground = 0xFFFFFF, background = 0x000000}

  --[[ Resizes the internal buffer and trims / expands it as necessary. ]]
  local function resize(w, h)
    -- Trim vertical area.
    for line = size.y, h + 1, -1 do
      buffer[line] = nil
    end
    -- Trim horizontal area.
    for line = 1, h do
      local buffer = buffer[line]
      for column = size.x, w + 1, -1 do
        buffer[column] = nil
      end
    end
    -- Expand vertical area.
    for line = size.y + 1, h do
      buffer[line] = {}
    end
    -- Expand horizontal area.
    for line = 1, h do
      local buffer = buffer[line]
      for column = line <= h and size.x + 1 or 1, w do
        buffer[column] = {" ", 0xFFFFFF, 0x000000}
      end
    end
    -- Store new sizes.
    size.x = x
    size.y = y
  end

  -- Pre-fill the buffer.
  resize(80, 24)

  -- Checked buffer access for abstraction layer below.
  local function get(x, y)
    if x < 1 or x > size.x then
      error("column out of bounds")
    elseif y < 1 or y > size.y then
      error("line out of bounds")
    else
      return buffer[x][y]
    end
  end

  -- Abstraction layer to avoid direct access to internals while re-using
  -- function instances.
  local term = {
    getSize = function() return size.x, size.y end,
    getCharAt = function(x, y) return get(x, y)[1] end,
    setCharAt = function(x, y, char)
      assert(type(char) == "string" and char:len() == 1,
        "'char' must be a single character")
      get(x, y)[1] = char
    end,
    getForegroundAt = function(x, y) return get(x, y)[2] end,
    setForegroundAt = function(x, y, color)
      assert(type(color) == "number" and color == math.floor(color),
        "'color' must be an integral number")
      get(x, y)[2] = color
    end,
    getBackgroundAt = function(x, y) return get(x, y)[3] end,
    setBackgroundAt = function(x, y, color)
      assert(type(color) == "number" and color == math.floor(color),
        "'color' must be an integral number")
      get(x, y)[3] = color
    end,
    getCursorPosition = function() return cursor.x, cursor.y end,
    setCursorPosition = function(x, y)
      assert(type(x) == "number" and x == math.floor(x),
        "'x' must be an integral number")
      assert(x >= 1 and x <= size.x, "column out of bounds")
      assert(type(y) == "number" and y == math.floor(y),
        "'y' must be an integral number")
      assert(y >= 1 and y <= size.y, "line out of bounds")
      cursor.x = x
      cursor.y = y
    end,
    getCursorBlink = function() return cursor.blink end,
    setCursorBlink = function(blink)
      assert(type(blink) == "boolean", "'blink' must be a boolean")
      cursor.blink = blink
    end,
    getForeground = function() return colors.foreground end,
    setForeground = function(color)
      assert(type(color) == "number" and color == math.floor(color),
        "'color' must be an integral number")
      colors.foreground = color
    end,
    getBackground = function() return colors.background end,
    setBackground = function(color)
      assert(type(color) == "number" and color == math.floor(color),
        "'color' must be an integral number")
      colors.background = color
    end
  }
  return setmetatable({}, {__index = term})
end


local function clear()
end