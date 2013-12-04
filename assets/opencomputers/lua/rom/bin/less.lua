local args = shell.parse(...)
if #args == 0 then
  print("Usage: less <filename1>")
  return
end

local file, reason = io.open(shell.resolve(args[1]))
if not file then
  print(reason)
  return
end

local line = nil
while true do
  local w, h = component.gpu.getResolution()
  term.clear()
  term.setCursorBlink(false)
  local i = 1
  while i < h do
    if not line then
      line = file:read("*l")
      if not line then -- eof
        return 
      end
    end
    if unicode.len(line) > w then
      print(unicode.sub(line, 1, w))
      line = unicode.sub(line, w + 1)
    else
      print(line)
      line = nil
    end
    i = i + 1
  end
  term.setCursor(1, h)
  term.write(":")
  term.setCursorBlink(true)
  while true do
    local event, address, char, code = coroutine.yield("key_down")
    if component.isPrimary(address) then
      if code == keyboard.keys.q then
        term.setCursorBlink(false)
        term.clearLine()
        return
      elseif code == keyboard.keys.space then
        break
      end
    end
  end
end
