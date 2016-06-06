function start()
    os.spawn("/bin/sshd.lua")
    print("\x1b[32m>>\x1b[39m Starting sshd")
end