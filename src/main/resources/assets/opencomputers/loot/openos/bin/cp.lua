local shell = require("shell")
local transfer = require("tools/transfer")

local args, options = shell.parse(...)
options.h = options.h or options.help
if #args < 2 or options.h then
  io.write("Usage: cp [-inrv] <from...> <to>\n")
  io.write(" -i: prompt before overwrite (overrides -n option).\n")
  io.write(" -n: do not overwrite an existing file.\n")
  io.write(" -r: copy directories recursively.\n")
  io.write(" -u: copy only when the SOURCE file differs from the destination\n")
  io.write("     file or when the destination file is missing.\n")
  io.write(" -P: preserve attributes, e.g. symbolic links.\n")
  io.write(" -v: verbose output.\n")
  io.write(" -x: stay on original source file system.\n")
  return not not options.h
end

-- clean options for copy (as opposed to move)
options = 
{
  cmd = "cp",
  i = options.i,
  n = options.n,
  r = options.r,
  u = options.u,
  P = options.P,
  v = options.v,
  x = options.x,
}

return transfer.batch(args, options)
