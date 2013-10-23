local args = shell.parse(...)
if #args < 2 then
  print("Usage: cp <from> <to>")
  return
end

local from, reason = shell.resolve(args[1])
local to, reason = shell.resolve(args[2])
local result, reason = fs.copy(from, to)
if not result then
  print(reason)
end
