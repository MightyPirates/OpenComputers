local io = {}

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
    require("process").info().data.io_input = file
  end
  return require("process").info().data.io_input
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
  local stream, result = require("filesystem").open(path, mode)
  if stream then
    return require("buffer").new(mode, stream)
  else
    return nil, result
  end
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
    require("process").info().data.io_output = file
  end
  return require("process").info().data.io_output
end

-- TODO io.popen = function(prog, mode) end

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

return io
