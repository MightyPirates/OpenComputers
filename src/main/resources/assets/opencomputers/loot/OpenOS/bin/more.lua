local component = require("component")
local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

local function nextLine(file, line, num)
  local w, h = component.gpu.getResolution()
  term.setCursorBlink(false)
  local i = 1
  while i < num do
    if not line then
      line = file:read("*l")
      if not line then -- eof
        return true
      end
    end
    local wrapped
    wrapped, line = text.wrap(text.detab(line), w, w)
    io.write(wrapped .. "\n")
    i = i + 1
  end
  term.setCursor(1, h)
  term.write(":")
  term.setCursorBlink(true)
  return false
end

local args = shell.parse(...)
if #args == 0 then
  io.write("Usage: more <filename1>")
  return
end

local file, reason = io.open(shell.resolve(args[1]))
if not file then
  io.stderr:write(reason)
  return
end

local line = nil
while true do
  local w, h = component.gpu.getResolution()
  term.clear()
  local num = h
  local r = nextLine(file, line, num)
  if r then
    return
  end
  while true do
    local event, address, char, code = coroutine.yield("key_down")
    if component.isPrimary(address) then
      if code == keyboard.keys.q then
        term.setCursorBlink(false)
        term.clearLine()
        return
      elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
        break
      elseif code == keyboard.keys.enter or code == keyboard.keys.down then
        term.clearLine()
        nextLine(file, line, 2)
      end
    end
  end
end
