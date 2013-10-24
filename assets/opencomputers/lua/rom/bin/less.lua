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
  local w, h = gpu.resolution()
  term.clear()
  term.cursorBlink(false)
  local i = 1
  while i < h do
    if not line then
      line = file:read("*l")
      if not line then -- eof
        return 
      end
    end
    if line:ulen() > w then
      print(line:usub(1, w))
      line = line:usub(w + 1)
    else
      print(line)
      line = nil
    end
    i = i + 1
  end
  term.cursor(1, h)
  term.write(":")
  term.cursorBlink(true)
  local event, address, char, code = event.wait("key_down")
  if component.isPrimary(address) then
    if code == keyboard.keys.q then
      term.cursorBlink(false)
      term.clearLine()
      return
    end
  end
end
