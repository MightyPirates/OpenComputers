local args = table.pack(...)
if args.n == 0 then
  print("Usage: rm <filename1> [<filename2> [...]]")
  return
end

for i = 1, args.n do
  local path = shell.resolve(args[i])
  if not fs.remove(path) then
    print(path .. ": no such file, or permission denied")
  end
end
