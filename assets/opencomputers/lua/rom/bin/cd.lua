local args = shell.parse(...)
if #args == 0 then
  print("Usage: cd <dirname>")
else
  local result, reason = shell.setWorkingDirectory(shell.resolve(args[1]))
  if not result then
    print(reason)
  end
end
