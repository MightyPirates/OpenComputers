local args, options = require("shell").parse(...)
if options.help then
  print([[`echo` writes the provided string(s) to the standard output.
  -n      do not output the trialing newline
  --help  display this help and exit]])
  return
end
io.write(table.concat(args," "))
if not options.n then
  print()
end
