function start()
    if component.invoke(computer.getBootAddress(), "isReadOnly") or (kernel.modules.special and kernel.modules.special.roroot) then
        local cow = kernel.modules.cowfs.new(computer.getBootAddress(), computer.tmpAddress())
        kernel.modules.vfs.mount(cow, "/")
    else
        kernel.modules.vfs.mount(computer.getBootAddress(), "/")
    end
    
    kernel.modules.vfs.mount(computer.tmpAddress(), "/tmp")
end
