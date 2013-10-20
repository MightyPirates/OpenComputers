local args = table.pack(...)
if args.n == 0 then
  print("Usage: which <program>")
  return
end
local result, reason = shell.which(args[1])
if result then
  print(result)
else
  print(reason)
end
