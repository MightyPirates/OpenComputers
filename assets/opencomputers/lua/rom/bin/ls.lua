local args = table.pack(...)
local path = shell.resolve(args[1] or ".")

local list, reason = fs.dir(path)
if not list then
  print(reason)
  return
end

for f in list do
  io.write(f .. "\t")
end
print()