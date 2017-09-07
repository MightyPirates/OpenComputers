local args, options = require("shell").parse(...)
if options.help then
  io.write([[
`echo` writes the provided string(s) to the standard output.
  -n      do not output the trialing newline
  -e      enable interpretation of backslash escapes
  --help  display this help and exit
]])
  return
end
if options.e then
  for index,arg in ipairs(args) do
    -- use lua load here to interpret escape sequences such as \27
    -- instead of writing my own language to interpret them myself
    -- note that in a real terminal, \e is used for \27
    args[index] = assert(load("return \"" .. arg:gsub('"', [[\"]]) .. "\""))()
  end
end
io.write(table.concat(args," "))
if not options.n then
  io.write("\n")
end
