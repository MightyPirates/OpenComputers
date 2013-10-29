local args = shell.parse(...)
if #args == 0 then
  print("Usage: rm <filename1> [<filename2> [...]]")
  return
end

for i = 1, #args do
  local path = shell.resolve(args[i])
  if not os.remove(path) then
    print(path .. ": no such file, or permission denied")
  end
end
