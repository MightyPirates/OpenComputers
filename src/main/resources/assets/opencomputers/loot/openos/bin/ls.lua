local shell = require("shell")

local _, ops = shell.parse(...)

if ops.help then
  print([[Usage: ls [OPTION]... [FILE]...
  -a, --all                  do not ignore entries starting with .
      --full-time            with -l, print time in full iso format
  -h, --human-readable       with -l and/or -s, print human readable sizes
      --si                   likewise, but use powers of 1000 not 1024
  -l                         use a long listing format
  -r, --reverse              reverse order while sorting
  -R, --recursive            list subdirectories recursively
  -S                         sort by file size
  -t                         sort by modification time, newest first
  -X                         sort alphabetically by entry extension
  -1                         list one file per line
  -p                         append / indicator to directories
  -M                         display Microsoft-style file and directory count after listing
      --no-color             Do not colorize the output (default colorized)
      --help                 display this help and exit
For more info run: man ls]])
  return 0
end

-- load complex, if we can (might be low on memory)
local full_ls_path = package.searchpath("tools/full-ls", package.path)
assert(full_ls_path, "could not find ls libraries")
local full_ls, reason = loadfile(full_ls_path, "bt", _G)
if not full_ls then
  io.stderr:write(tostring(reason).."\nFor low memory systems, try using `list` instead\n")
  return 1
end
return full_ls(...)
