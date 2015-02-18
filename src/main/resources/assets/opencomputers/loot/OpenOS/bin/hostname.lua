local args = {...}
if args[1] then
  local file = io.open("/etc/hostname", "w")
  file:write(args[1])
  file:close()
  os.setenv("HOSTNAME", args[1])
  os.setenv("PS1", "$HOSTNAME:$PWD# ")
else
  local file = io.open("/etc/hostname")
  if file then
    io.write(file:read("*l"))
    file:close()
  end
end
