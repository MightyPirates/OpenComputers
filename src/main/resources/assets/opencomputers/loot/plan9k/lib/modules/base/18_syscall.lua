kernel.userspace.package.preload.pipes = {}

kernel.userspace.package.preload.pipes.setKernelOutput = function(sink)
    kernel.io.println = function(str)
        sink:setvbuf("line")
        sink:write("[dmesg] ")
        sink:write(tostring(str))
        sink:write("\n")
        sink:setvbuf("no")
        sink:flush()
    end
end

function start()
    kernel.userspace.package.preload.pipes.joinThread = kernel.modules.threadUtil.joinThread
    kernel.userspace.package.preload.pipes.getThreadInfo = kernel.modules.threadUtil.getThreadInfo
    kernel.userspace.package.preload.pipes.setKillHandler = kernel.modules.threadUtil.setKillHandler
    kernel.userspace.package.preload.pipes.getPid = function()
        return kernel.modules.threading.currentThread.pid, kernel.modules.threading.currentThread.uid
    end
    
    kernel.userspace.package.preload.pipes.shouldYield = kernel.modules.threading.checkTimeout
    kernel.userspace.package.preload.pipes.setTimer = kernel.modules.timer.add
    kernel.userspace.package.preload.pipes.removeTimer = kernel.modules.timer.remove
    kernel.userspace.package.preload.pipes.setThreadName = function(name)
        kernel.modules.threading.currentThread.name = name
    end
    kernel.userspace.package.preload.pipes.log = function(msg)
        kernel.io.println(msg)
    end
    
    kernel.userspace.package.preload.pipes.openPty = kernel.modules.pty.new
    kernel.userspace.package.preload.pipes.cowProxy = kernel.modules.cowfs.new
    
    kernel.userspace.package.preload.pipes.wrapIPC = kernel.modules.ipc.wrap
    
    kernel.userspace.package.preload.pipes.setns = kernel.modules.cgroups.new
end