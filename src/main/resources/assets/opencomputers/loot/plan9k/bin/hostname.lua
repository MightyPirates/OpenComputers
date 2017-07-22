local args = {...}
if args[1] then
  local file, reason = io.open("/etc/hostname", "w")
  if not file then
    io.stderr:write(reason .. "\n")
  else
    file:write(args[1])
    file:close()
    os.setenv("HOSTNAME", args[1])
    os.setenv("PS1", "\x1b[33m$HOSTNAME\x1b[32m:\x1b[33m$PWD\x1b[31m#\x1b[39m ")
    computer.pushSignal("hostname", args[1])
  end
else
  local file = io.open("/etc/hostname")
  if file then
    io.write(file:read("*l"), "\n")
    file:close()
  else
    io.stderr:write("Hostname not set\n")
  end
end
