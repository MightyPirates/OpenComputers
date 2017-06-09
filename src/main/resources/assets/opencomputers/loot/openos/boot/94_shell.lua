-- there doesn't seem to be a reason to update $HOSTNAME after the init signal
-- as user space /etc/profile comes after this point anyways
loadfile("/bin/hostname.lua")("--update")
os.setenv("SHELL","/bin/sh.lua")
