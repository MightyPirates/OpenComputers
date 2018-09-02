local keyboard = require("keyboard")
local shell = require("shell")
local term = require("term")

local args = shell.parse(...)
if #args > 1 then
  io.write("Usage: more <filename>\n")
  io.write("- or no args reads stdin\n")
  return 1
end

local cat_cmd = table.concat({"cat", ...}, " ")
if not io.output().tty then
  return os.execute(cat_cmd)
end

term.clear()

local preader = io.popen(cat_cmd)
local intercept = true
while true do
  local _, height, _, _, _, y = term.getViewport()
  if intercept and y == height then
    term.clearLine()
    io.write(":") -- status
    ::INPUT::
    local _, _, _, code = term.pull("key_down")
    if code == keyboard.keys.q then
      term.clearLine()
      os.exit(1) -- abort
    elseif code == keyboard.keys["end"] then
      intercept = false
    elseif code == keyboard.keys.space or code == keyboard.keys.pageDown then
      term.clear() -- clear whole screen, get new page drawn; move cursor to 1,1
    elseif code == keyboard.keys.enter or code == keyboard.keys.down then
      term.clearLine() -- remove status bar
      term.scroll(1) -- move everything up one
      term.setCursor(1, height - 1)
    else
      goto INPUT
    end
  end
  print(preader:read() or os.exit())
end
