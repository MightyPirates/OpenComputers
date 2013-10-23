local args = shell.parse(...)
if #args == 0 then
  print("Usage: cd <dirname>")
else
  local result, reason = shell.cwd(shell.resolve(args[1]))
  if not result then
    print(reason)
  end
end
