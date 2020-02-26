local io = {}

-------------------------------------------------------------------------------

function io.close(file)
  return (file or io.output()):close()
end

function io.flush()
  return io.output():flush()
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
  local resolved_path = require("shell").resolve(path)
  local stream, result = require("filesystem").open(resolved_path, mode)
  if stream then
    return require("buffer").new(mode, stream)
  else
    return nil, result
  end
end

function io.stream(fd,file,mode)
  checkArg(1,fd,'number')
  checkArg(2, file, "table", "string", "nil")
  assert(fd>=0,'fd must be >= 0. 0 is input, 1 is stdout, 2 is stderr')
  local dio = require("process").info().data.io
  if file then
    if type(file) == "string" then
      file = assert(io.open(file, mode))
    end
    dio[fd] = file
  end
  return dio[fd]
end

function io.input(file)
  return io.stream(0, file, 'r')
end

function io.output(file)
  return io.stream(1, file,'w')
end

function io.error(file)
  return io.stream(2, file,'w')
end

function io.popen(prog, mode, env)
  return require("pipe").popen(prog, mode, env)
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

local dup_mt =   {__index = function(dfd, key)
  local fd_value = dfd.fd[key]
  if key ~= "close" and type(fd_value) ~= "function" then return fd_value end
  return function(self, ...)
    if key == "close" or self._closed then self._closed = true return end
    return fd_value(self.fd, ...)
  end
end, __newindex = function(dfd, key, value)
  dfd.fd[key] = value
end}

function io.dup(fd)
  return setmetatable({fd=fd,_closed=false}, dup_mt)
end

-------------------------------------------------------------------------------

return io
