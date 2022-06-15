local shell = require("shell")
local fs = require("filesystem")

local args = shell.parse(...)
if #args == 0 then
  args = {"-"}
end

local input_method, input_param = "read", require("tty").getViewport()

for i = 1, #args do
  local arg = shell.resolve(args[i])
  if fs.isDirectory(arg) then
    io.stderr:write(string.format('cat %s: Is a directory\n', arg))
    os.exit(1)
  else
    local file, reason
    if args[i] == "-" then
      file, reason = io.stdin, "missing stdin"
      input_method, input_param = "readLine", false
    else
      file, reason = fs.open(arg)
    end
    if not file then
      io.stderr:write(string.format("cat: %s: %s\n", args[i], tostring(reason)))
      os.exit(1)
    else
      repeat
        local chunk = file[input_method](file, input_param)
        if chunk then
          io.write(chunk)
        end
      until not chunk
      file:close()
    end
  end
end
