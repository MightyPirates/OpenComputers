local args = table.pack(...)
if args.n < 1 then
  print("Usage: unalias <name>")
  return
end

shell.alias(args[1], nil)
