local fs = require("filesystem")
local shell = require("shell")
local sh = require("sh")

local touch = loadfile(shell.resolve("touch", "lua"))
local mkdir = loadfile(shell.resolve("mkdir", "lua"))

if not touch then
  local errorMessage = "missing tools for mktmp"
  io.stderr:write(errorMessage .. '\n')
  return false, errorMessage
end

local args, ops = shell.parse(...)

local function pop(...)
  local result
  for _,key in ipairs({...}) do
    result = ops[key] or result
    ops[key] = nil
  end
  return result
end

local directory = pop('d')
local verbose = pop('v', 'verbose')
local quiet = pop('q', 'quiet')

if pop('help') or #args > 1 or next(ops) then
  print([[Usage: mktmp [OPTION] [PATH]
Create a new file with a random name in $TMPDIR or PATH argument if given
  -d              create a directory instead of a file
  -v, --verbose   print result to stdout, even if no tty
  -q, --quiet     do not print results to stdout, even if tty (verbose overrides)
      --help      print this help message]])
  if next(ops) then
    io.stderr:write("invalid option: " .. (next(ops)) .. '\n')
    return 1
  end
  return
end

if not verbose then
  if not quiet then
    if io.stdout.tty then
      verbose = true
    end
  end
end

local prefix = args[1] or os.getenv("TMPDIR") .. '/'
if not fs.exists(prefix) then
  io.stderr:write(
    string.format(
      "cannot create tmp file or directory at %s, it does not exist\n",
      prefix))
  return 1
end

local tmp = os.tmpname()
local ok, reason = (directory and mkdir or touch)(tmp)

if sh.internal.command_passed(ok) then
  if verbose then
    print(tmp)
  end
  return tmp
end

return ok, reason
