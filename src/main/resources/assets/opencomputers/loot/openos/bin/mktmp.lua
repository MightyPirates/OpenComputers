local fs = require("filesystem")
local uuid = require("uuid")
local shell = require("shell")
local sh = require("sh")

local touch = loadfile(shell.resolve("touch", "lua"))
local mkdir = loadfile(shell.resolve("mkdir", "lua"))

if not uuid or not touch then
  local errorMessage = "missing tools for mktmp"
  io.stderr:write(errorMessage .. '\n')
  return false, errorMessage
end

local args, ops = shell.parse(...)

local function pop(key)
  local result = ops[key]
  ops[key] = nil
  return result
end

local directory = pop('d')
local verbose = pop('v')
verbose = pop('verbose') or verbose
local quiet = pop('q') or quiet
quiet = pop('quiet') or quiet

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

while true do
  local tmp = prefix .. uuid.next()
  if not fs.exists(tmp) then

    local ok, reason
    if directory then
      ok, reason = mkdir(tmp)
    else
      ok, reason = touch(tmp)
    end

    if sh.internal.command_passed(ok) then
      if verbose then
        print(tmp)
      end
      return tmp
    else
      return ok, reason
    end
  end
end
