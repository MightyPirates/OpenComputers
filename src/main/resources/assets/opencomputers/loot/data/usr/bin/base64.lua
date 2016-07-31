local shell = require("shell")
local data = require("data")


local args, parms = shell.parse(...)
if parms.h or parms.help then
  io.stderr:write("See: man base64" .. "\n")
  os.exit(true)
end
local encodingfun = nil
local encode
if parms.d or parms.decode then
  encodingfun = data.decode64
  encode = false
else
  encodingfun = data.encode64
  encode = true
end

if #args == 0 then
  repeat
    local read = io.read(encode and 3 or 4)
    if read then
      io.write(encodingfun(read))
    end
  until not read
else
  for i = 1, #args do
    local file, reason = io.open(shell.resolve(args[i]))
    if not file then
      io.stderr:write(tostring(reason) .. "\n")
      os.exit(false)
    end
    repeat
      local line = file:read(encode and 3 or 4)
      if line then
        io.write(encodingfun(line))
      end
    until not line
    file:close()
  end
end
