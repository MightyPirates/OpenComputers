local args = table.pack(...)
if args.n == 0 then
  print(shell.cwd())
else
  local result, reason = shell.cwd(shell.resolve(args[1]))
  if not result then
    print(reason)
  end
end
