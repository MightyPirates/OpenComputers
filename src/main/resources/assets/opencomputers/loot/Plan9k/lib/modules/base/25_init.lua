kernel.io.println("Installing init thread")

kernel.modules.threading.spawn(function()
    kernel.io.println("Execute init")
    
    kernel._G.dofile("/bin/init.lua", kernel._G)
    kernel.io.println("Init is dead")
    kernel.panic()
end, 0, "[init]")

