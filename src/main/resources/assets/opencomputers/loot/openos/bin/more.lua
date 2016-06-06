local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

local args = shell.parse(...)
if #args > 1 then
  io.write("Usage: more <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end
local arg = args[1] or "-"
local file, reason
if arg == "-" then
  file, reason = io.stdin, "this process has no stdin"
else
  file, reason = io.open(shell.resolve(arg))
end
if not file then
  io.stderr:write(reason,'\n')
  return 1
end

local line = nil
local function readlines(num)
  local x, y, w, h = term.getGlobalArea()
  num = num or (h - 1)
  for _ = 1, num do
    if not line then
      line = file:read("*l")
      if not line then -- eof
        return nil
      end
    end
    local wrapped
    wrapped, line = text.wrap(text.detab(line), w, w)
    io.write(wrapped,"\n")
  end
  term.setCursor(1, h)
  term.write(":")
  return true
end

while true do
  term.clear()
  if not readlines() then
    return
  end
  while true do
    local event, address, char, code = term.pull("key_down")
    if code == keyboard.keys.q then
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
