local args = table.pack(...)
local options = {}
local dirs = {}
for i = 1, args.n do
  if args[i]:usub(1, 1) == "-" then
    if args[i]:ulen() > 1 then
      options[args[i]:usub(2)] = true
    end
  else
    table.insert(dirs, args[i])
  end
end
if #dirs == 0 then
  table.insert(dirs, ".")
end

for i = 1, #dirs do
  local path = shell.resolve(dirs[i])

  local list, reason = fs.dir(path)
  if not list then
    print(reason)
  else
    for f in list do
      if options.l then
        print(f, fs.size(fs.concat(path, f)))
      else
        io.write(f .. "\t")
      end
    end
    print()
  end
end