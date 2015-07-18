local shell = require("shell")

local args = shell.parse(...)
if #args == 0 then
  repeat
    local read = io.read("*L")
    if read then
      io.write(read)
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
      local blob = file:read(1024)
      if blob then
        io.write(blob)
        os.sleep( 0 )
      end
    until not blob
    file:close()
  end
end
