-- there doesn't seem to be a reason to update $HOSTNAME after the init signal
-- as user space /etc/profile comes after this point anyways
if require("filesystem").exists("/etc/hostname") then
  loadfile("/bin/hostname.lua")("--update")
end
os.setenv("SHELL","/bin/sh.lua")
