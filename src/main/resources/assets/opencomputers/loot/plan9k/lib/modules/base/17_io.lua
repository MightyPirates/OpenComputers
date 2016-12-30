local io = {}
local threading
-------------------------------------------------------------------------------

local function buffWrap(b)
    return setmetatable({
        _closed = false, 
        close = function() _closed = true end
        }, { __index = function(t, k)
            return type(b[k]) == "function" and function(...)
                if not t._closed then
                    return b[k](...)
                end
                return nil, "File is closed"
            end or b[k]
        end})
end

-------------------------------------------------------------------------------

function io.close(file)
  return (file or io.output()):close()
end

function io.flush()
  return io.output():flush()
end

function io.input(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file)
      if not result then
        error(reason, 2)
      end
      file = result
    elseif not io.type(file) then
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")", 2)
    end
    threading.currentThread.io_input = file
  end
  return threading.currentThread.io_input
end

function io.lines(filename, ...)
  if filename then
    local file, reason = io.open(filename)
    if not file then
      error(reason, 2)
    end
    local args = table.pack(...)
    return function()
      local result = table.pack(file:read(table.unpack(args, 1, args.n)))
      if not result[1] then
        if result[2] then
          error(result[2], 2)
        else -- eof
          file:close()
          return nil
        end
      end
      return table.unpack(result, 1, result.n)
    end
  else
    return io.input():lines()
  end
end

function io.open(path, mode)
  -- These requires are not on top because this is a bootstrapped file.
  if path == "-" then
    if mode:sub(1, 1) == "r" then
        return buffWrap(io.input())
    else
        return buffWrap(io.output())
    end
  end
  local stream, result = kernel.modules.vfs.open(path, mode)
  if stream then
    return kernel.modules.buffer.new(mode, stream)
  else
    return nil, result
  end
end

function io.popen(prog, mode, ...)
    local name = "unknown"
    if type(prog) == "string" then
        name = prog
        prog, reason = kernel._G.loadfile(prog, nil, kernel._G)
        if not prog then
            error(reason)
        end
    end
    
    if mode == "w" then
        local newin, sink = kernel.modules.buffer.pipe()
        local thread = threading.spawn(prog, 0, name, _, _, ...) --TODO: child mode somehow
        
        thread.io_output = threading.currentThread.io_output
        thread.io_error = threading.currentThread.io_error
        thread.io_input = newin
        
        sink.thread = thread.pid
        return sink
    elseif mode == "r" or mode == nil then
        local out, newout = kernel.modules.buffer.pipe()
        
        local thread = threading.spawn(prog, 0, name, _, _, ...) --TODO: child mode somehow
        thread.io_output = newout
        thread.io_error = threading.currentThread.io_error
        thread.io_input = threading.currentThread.io_input
        
        out.thread = thread.pid
        return out
    elseif mode == "rw" or mode == "wr" then
        local newin, sink = kernel.modules.buffer.pipe()
        local out, newout = kernel.modules.buffer.pipe()
        
        local thread = threading.spawn(prog, 0, name, _, _, ...) --TODO: child mode somehow
        thread.io_output = newout
        thread.io_error = threading.currentThread.io_error
        thread.io_input = newin
        
        sink.thread = thread.pid
        out.thread = thread.pid
        
        return sink, out
    elseif mode == "" then
        local thread = threading.spawn(prog, 0, name, _, _, ...) --TODO: child mode somehow
        
        thread.io_output = threading.currentThread.io_output
        thread.io_error = threading.currentThread.io_error
        thread.io_input = threading.currentThread.io_input
        
        return thread.pid
    end
    return nil, "Unallowed mode"
end

function io.pipe()
    return kernel.modules.buffer.pipe()
end

function io.output(file)
  if file then
    if type(file) == "string" then
      local result, reason = io.open(file, "w")
      if not result then
        error(reason, 2)
      end
      file = result
    elseif not io.type(file) then
      error("bad argument #1 (string or file expected, got " .. type(file) .. ")", 2)
    end
    threading.currentThread.io_output = file
  end
  return threading.currentThread.io_output
end

function io.read(...)
  return io.input():read(...)
end

function io.tmpfile()
  local name = os.tmpname()
  if name then
    return io.open(name, "a")
  end
end

function io.type(object)
  if type(object) == "table" then
    if getmetatable(object) == "file" then
      if object.stream.handle then
        return "file"
      else
        return "closed file"
      end
    end
  end
  return nil
end

function io.write(...)
  return io.output():write(...)
end

-------------------------------------------------------------------------------

kernel.userspace.io = io

setmetatable(io, {__index = function(_, k)
    if k == "stdout" then return io.output()
    elseif k == "stdin" then return io.input()
    elseif k == "stderr" then return kernel.modules.threading.currentThread.io_error
    end
end})

_G.io = io

function start()
    threading = kernel.modules.threading
end
