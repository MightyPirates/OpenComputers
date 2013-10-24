local args = shell.parse(...)
if #args == 0 then
  print("Usage: cat <filename1> [<filename2> [...]]")
  return
end

for i = 1, #args do
  local file, reason = io.open(shell.resolve(args[i]))
  if not file then
    print(reason)
    return
  end
  repeat
    local line = file:read()
    if line then
      print(line)
    end
  until not line
end
