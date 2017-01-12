filesystems = {}

kernel.modules.sysfs.data.mount = {
     __type = "f",
     write = function(h, data)
         local args = kernel.modules.util.split(data, "%s")
         kernel.io.println("Mount " .. table.concat(args, ","))
         if #args < 3 then
             return nil, "Invalid argument count"
         end
         if not filesystems[args[1]] then
             return nil, "Invalid filesystem"
         end
         
         local proxy, err = filesystems[args[1]](table.unpack(args, 2, #args - 1))
         if not proxy then
             kernel.io.println("Error mounting " .. tostring(args[1]) .. ": " .. tostring(err))
             return nil, err
         end
         
         return kernel.modules.vfs.mount(proxy, args[#args])
     end
}