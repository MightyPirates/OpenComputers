function start()
    print("\x1b[32m>>\x1b[39m Auto-update")
    os.spawn("/usr/bin/mpt.lua", "-Yyu")
end
