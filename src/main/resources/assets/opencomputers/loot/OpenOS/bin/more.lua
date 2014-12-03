local component = require("component")
local event = require("event")
local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

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
local function readlines(num)
  local w, h = component.gpu.getResolution()
  num = num or (h - 1)
  term.setCursorBlink(false)
  for _ = 1, num do
    if not line then
      line = file:read("*l")
      if not line then -- eof
        return nil
      end
    end
    local wrapped
    wrapped, line = text.wrap(text.detab(line), w, w)
    io.write(wrapped .. "\n")
  end
  term.setCursor(1, h)
  term.write(":")
  term.setCursorBlink(true)
  return true
end

while true do
  term.clear()
  if not readlines() then
    return
  end
  while true do
    local event, address, char, code = event.pull("key_down")
    if component.isPrimary(address) then
      if code == keyboard.keys.q then
        term.setCursorBlink(false)
        term.clearLine()
        return
      elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
        break
      elseif code == keyboard.keys.enter or code == keyboard.keys.down then
        term.clearLine()
        if not readlines(1) then
          return
        end
      end
    end
  end
end
