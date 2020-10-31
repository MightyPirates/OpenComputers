local shell = require("shell")
local transfer = require("tools/transfer")

local args, options = shell.parse(...)
options.h = options.h or options.help
if #args < 2 or options.h then
  io.write([[Usage: cp [OPTIONS] <from...> <to>
 -i: prompt before overwrite (overrides -n option).
 -n: do not overwrite an existing file.
 -r: copy directories recursively.
 -u: copy only when the SOURCE file differs from the destination
     file or when the destination file is missing.
 -P: preserve attributes, e.g. symbolic links.
 -v: verbose output.
 -x: stay on original source file system.
 --skip=P: skip files matching lua regex P
]])
  return not not options.h
end

-- clean options for copy (as opposed to move)
options =
{
  cmd = "cp",
  i = options.i,
  f = options.f,
  n = options.n,
  r = options.r,
  u = options.u,
  P = options.P,
  v = options.v,
  x = options.x,
  skip = {options.skip},
}

return transfer.batch(args, options)
