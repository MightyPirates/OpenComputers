local args = shell.parse(...)
if #args < 2 then
  print("Usage: mv <from> <to>")
  return
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
local result, reason = os.rename(from, to)
if not result then
  print(reason)
end
