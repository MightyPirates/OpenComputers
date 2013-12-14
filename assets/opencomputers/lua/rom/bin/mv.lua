local args, options = shell.parse(...)
if #args < 2 then
  print("Usage: mv [-f] <from> <to>")
  print(" -f: overwrite file if it already exists.")
  return
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end
if fs.exists(to) then
  if not options.f then
    error("target file exists")
  end
  fs.remove(to)
end
local result, reason = os.rename(from, to)
if not result then
  print(reason or "unknown error")
end
