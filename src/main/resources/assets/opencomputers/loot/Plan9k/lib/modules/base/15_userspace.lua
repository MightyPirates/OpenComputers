kernel.userspace = setmetatable({}, {__index = kernel._K})

kernel.userspace.computer = {}

kernel.userspace.computer.address = kernel._K.computer.address
kernel.userspace.computer.tmpAddress = kernel._K.computer.tmpAddress
kernel.userspace.computer.freeMemory = kernel._K.computer.freeMemory
kernel.userspace.computer.totalMemory = kernel._K.computer.totalMemory
kernel.userspace.computer.energy = kernel._K.computer.energy
kernel.userspace.computer.maxEnergy = kernel._K.computer.maxEnergy
kernel.userspace.computer.isAvailable = kernel._K.computer.isAvailable
kernel.userspace.computer.users = kernel._K.computer.users
kernel.userspace.computer.addUser = kernel._K.computer.addUser
kernel.userspace.computer.removeUser = kernel._K.computer.removeUser
kernel.userspace.computer.pushSignal = kernel._K.computer.pushSignal
kernel.userspace.computer.uptime = kernel._K.computer.uptime
kernel.userspace.computer.getBootAddress = kernel._K.computer.getBootAddress

kernel.userspace.computer.shutdown = kernel.modules.gc.shutdown

kernel.userspace.computer.pullSignal = function(timeout)
    return coroutine.yield("signal", timeout)
end

kernel.userspace.computer.hasSignal = function(sigType)
    for _,v in ipairs(kernel.modules.threading.currentThread.eventQueue) do
        if v[1] == (sigType or "signal") then
            return true
        end
    end
    return false
end

kernel.userspace.coroutine = {}

kernel.userspace.os = setmetatable({}, {__index = kernel._K.os})

kernel.userspace.os.remove = kernel.modules.vfs.remove
kernel.userspace.os.rename = kernel.modules.vfs.rename

function kernel.userspace.os.spawn(prog, ...)
    local isThread = type(prog) == "function"
    local name = isThread and kernel.modules.threading.currentThread.name or "unknown"
    if type(prog) == "string" then
        name = kernel.modules.vfs.resolve(prog)
        prog, reason = kernel._G.loadfile(prog, nil, kernel._G)
        if not prog then
            error(tostring(reason) .. ": " .. tostring(prog))
        end
    end
    local thread = kernel.modules.threading.spawn(prog, 0, name, isThread, _, ...)
    thread.io_output = kernel.modules.threading.currentThread.io_output
    thread.io_input = kernel.modules.threading.currentThread.io_input
    thread.io_error = kernel.modules.threading.currentThread.io_error
    return thread.pid
end

function kernel.userspace.os.spawnp(prog, stdin, stdout, stderr, ...)
    local isThread = type(prog) == "function"
    local name = isThread and kernel.modules.threading.currentThread.name or "unknown"
    if type(prog) == "string" then
        name = kernel.modules.vfs.resolve(prog)
        prog, reason = kernel._G.loadfile(prog, nil, kernel._G)
        if not prog then
            error(tostring(reason) .. ": " .. tostring(prog))
        end
    end
    local thread = kernel.modules.threading.spawn(prog, 0, name, isThread, _, ...)
    thread.env["_"] = name
    thread.io_output = stdout --todo: check types!
    thread.io_error = stderr --todo: check types!
    thread.io_input = stdin
    return thread.pid
end

function kernel.userspace.os.kill(pid, signal)
    return kernel.modules.threadUtil.userKill(pid, signal or "terminate")
end

function kernel.userspace.os.exit()
    kernel.modules.threading.kill(kernel.modules.threading.currentThread.pid)
    coroutine.yield("yield", 0)
end

function kernel.userspace.os.sleep(time)
    coroutine.yield("yield", computer.uptime() + (time or 0))
end

function kernel.userspace.os.getenv(name)
    return kernel.modules.threading.currentThread.env[name]
end

function kernel.userspace.os.setenv(name, value)
    kernel.modules.threading.currentThread.env[name] = value
end

function kernel.userspace.dofile(filename, env)
  local program, reason = kernel.userspace.loadfile(filename, nil, env or kernel._G)
  if not program then
    return error(reason, 0)
  end
  return program()
end

function kernel.userspace.loadfile(filename, mode, env)
  local file, reason = kernel.modules.io.io.open(filename)
  if not file then
    return nil, reason
  end
  local source, reason = file:read("*a")
  file:close()
  if not source then
    return nil, reason
  end
  if string.sub(source, 1, 1) == "#" then
    local endline = string.find(source, "\n", 2, true)
    if endline then
      source = string.sub(source, endline + 1)
    else
      source = ""
    end
  end
  return kernel._G.load(source, "=" .. filename, mode, env or kernel._G)
end

function kernel.userspace.load(ld, source, mode, env)
    return load(ld, source, mode, env or kernel._G)
end

function kernel.userspace.print(...)
  local args = table.pack(...)
  kernel.modules.io.io.stdout:setvbuf("line")
  for i = 1, args.n do
    local arg = tostring(args[i])
    if i > 1 then
      arg = "\t" .. arg
    end
    kernel.modules.io.io.stdout:write(arg)
  end
  kernel.modules.io.io.stdout:write("\n")
  kernel.modules.io.io.stdout:setvbuf("no")
  kernel.modules.io.io.stdout:flush()
end

kernel.userspace.coroutine = {}

--lua 5.3 <-> 5.2 compat
kernel.userspace.bit32 = bit32 or load([[return {
    band = function(a, b) return a & b end,
    bor = function(a, b) return a | b end,
    bxor = function(a, b) return a ~ b end,
    bnot = function(a) return ~a end,
    rshift = function(a, n) return a >> n end,
    lshift = function(a, n) return a << n end,
}]])()
