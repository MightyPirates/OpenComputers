commands = {}

function execute(comm)
    local command = kernel.modules.util.split(comm," ")
    kernel.io.println("Execute: "..command[1])
    if commands[command[1]] then
        table.remove(command, 1)
        return commands[command[1]](table.unpack(command))
    end
    return "KCMD: Command not found!"
end

commands.shutdomn = kernel.modules.gc.shutdown