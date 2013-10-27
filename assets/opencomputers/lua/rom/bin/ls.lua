local dirs, options = shell.parse(...)
if #dirs == 0 then
  table.insert(dirs, ".")
end

for i = 1, #dirs do
  local path = shell.resolve(dirs[i])
  if #dirs > 1 then
    if i > 1 then print() end
    print("/" .. path .. ":")
  end
  local list, reason = fs.list(path)
  if not list then
    print(reason)
  else
    for f in list do
      if options.a or f:usub(1, 1) ~= "." then
        if options.l then
          print(f, fs.size(fs.concat(path, f)))
        else
          io.write(f .. "\t")
        end
      end
    end
    if not options.l then
      print()
    end
  end
end
