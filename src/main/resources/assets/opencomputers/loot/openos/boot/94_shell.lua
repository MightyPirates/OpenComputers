local shell = require("shell")

require("event").listen("init", function()
  local file = io.open("/etc/hostname")
  if file then
    os.setenv("HOSTNAME", file:read("*l"))
    os.setenv("PS1", "$HOSTNAME:$PWD# ")
    file:close()
  end
end)
