function dofile(filename)
  local program, reason = loadfile(filename)
  if not program then
    return error(reason, 0)
  end
  return program()
end

function loadfile(filename, mode, env)
  local file, reason = io.open(filename)
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
  return load(source, "=" .. filename, mode, env)
end

function print(...)
  local args = table.pack(...)
  io.stdout:setvbuf("line")
  for i = 1, args.n do
    local arg = tostring(args[i])
    if i > 1 then
      arg = "\t" .. arg
    end
    io.stdout:write(arg)
  end
  io.stdout:write("\n")
  io.stdout:setvbuf("no")
  io.stdout:flush()
end
