local args = {...}
if args[1] then
  local file, reason = io.open("/etc/hostname", "w")
  if not file then
    io.stderr:write(reason .. "\n")
    return 1
  else
    file:write(args[1])
    file:close()
    os.setenv("HOSTNAME", args[1])
    os.setenv("PS1", "$HOSTNAME:$PWD# ")
  end
else
  local file = io.open("/etc/hostname")
  if file then
    io.write(file:read("*l"), "\n")
    file:close()
  else
    io.stderr:write("Hostname not set\n")
    return 1
  end
end
