local args = {...}
if #args == 0 then
  repeat
    local read = require("term").read()
    if read then
      io.write(read)
    end
  until not read
else
  for i = 1, #args do
    local file, reason = io.open(args[i])
    if not file then
      io.stderr:write(reason .. "\n")
      return
    end
    repeat
      local line = file:read("*L")
      if line then
        io.write(line)
      end
    until not line
    file:close()
    io.write("\n")
  end
end
