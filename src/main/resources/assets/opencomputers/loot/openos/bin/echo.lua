local args, options = require("shell").parse(...)
if options.help then
  print([[`echo` writes the provided string(s) to the standard output.
  -n      do not output the trialing newline
  -e      enable interpretation of backslash escapes
  --help  display this help and exit]])
  return
end
if options.e then
  for index,arg in ipairs(args) do
    args[index] = assert(load("return \"" .. arg:gsub('"', [[\"]]) .. "\""))()
  end
end
io.write(table.concat(args," "))
if not options.n then
  print()
end
