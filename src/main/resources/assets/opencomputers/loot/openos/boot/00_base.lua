function loadfile(filename, ...)
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
    buffer[#buffer + 1] = data
  end
  return load(table.concat(buffer), "=" .. filename, ...)
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
  local pre = ""
  for i = 1, args.n do
    stdout:write(pre, tostring(args[i]))
    pre = "\t"
  end
  stdout:write("\n")
  stdout:setvbuf("no")
  stdout:flush()
end
