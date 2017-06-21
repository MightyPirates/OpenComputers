local shell = require("shell")
local args, ops = shell.parse(...)
local hostname = args[1]

if hostname then
  local file, reason = io.open("/etc/hostname", "w")
  if not file then
    io.stderr:write("failed to open for writing: ", reason, "\n")
    return 1
  end
  file:write(hostname)
  file:close()
  ops.update = true
else
  local file = io.open("/etc/hostname")
  if file then
    hostname = file:read("*l")
    file:close()
  end
end

if ops.update then
  os.setenv("HOSTNAME_SEPARATOR", hostname and #hostname > 0 and ":" or "")
  os.setenv("HOSTNAME", hostname)
elseif hostname then
  print(hostname)
else
  io.stderr:write("Hostname not set\n")
  return 1
end
