function loadfile(filename, mode, env)
  if filename:sub(1,1) ~= "/" then
    filename = (os.getenv("PWD") or "/") .. "/" .. filename
  end
  local handle, reason = require("filesystem").open(filename)
  if not handle then
    return nil, reason
  end
  local buffer = {}
  while true do
    local data, reason = handle:read(1024)
    if not data then
      handle:close()
      if reason then
        return nil, reason
      end
      break
    end
    table.insert(buffer, data)
  end
  buffer[1] = (buffer[1] or ""):gsub("^#![^\n]+", "") -- remove shebang if any
  buffer = table.concat(buffer)
  return load(buffer, "=" .. filename, mode, env)
end

function dofile(filename)
  local program, reason = loadfile(filename)
  if not program then
    return error(reason .. ':' .. filename, 0)
  end
  return program()
end

function print(...)
  local args = table.pack(...)
  local stdout = io.stdout
  stdout:setvbuf("line")
  for i = 1, args.n do
    local arg = tostring(args[i])
    if i > 1 then
      arg = "\t" .. arg
    end
    stdout:write(arg)
  end
  stdout:write("\n")
  stdout:setvbuf("no")
  stdout:flush()
end
