local shell = require("shell")
local fs =  require("filesystem")
local text = require("text")

local args, options = shell.parse(...)

local function usage()
  print(
[[Usage: rmdir [OPTION]... DIRECTORY...
Removes the DIRECTORY(ies), if they are empty.

  -q, --ignore-fail-on-non-empty
                  ignore failures due solely to non-empty directories
  -p, --parents   remove DIRECTORY and its empty ancestors
                  e.g. 'rmdir -p a/b/c' is similar to 'rmdir a/b/c a/b a'
  -v, --verbose   output a diagnostic for every directory processed
      --help      display this help and exit]])
end

if options.help then
  usage()
  return 0
end

if #args == 0 then
  io.stderr:write("rmdir: missing operand\n")
  return 1
end

options.p = options.p or options.parents
options.v = options.v or options.verbose
options.q = options.q or options['ignore-fail-on-non-empty']

local ec = 0
local function ec_bump()
  ec = 1
  return 1
end

local function remove(path, ...)
  -- check to end recursion
  if path == nil then
    return true
  end

  if options.v then
    print(string.format('rmdir: removing directory, %s', path))
  end

  local rpath = shell.resolve(path)
  if path == '.' then
    io.stderr:write('rmdir: failed to remove directory \'.\': Invalid argument\n')
    return ec_bump()
  elseif not fs.exists(rpath) then
    io.stderr:write("rmdir: cannot remove " .. path .. ": path does not exist\n")
    return ec_bump()
  elseif fs.isLink(rpath) or not fs.isDirectory(rpath) then
    io.stderr:write("rmdir: cannot remove " .. path .. ": not a directory\n")
    return ec_bump()
  else
    local list, reason = fs.list(rpath)

    if not list then
      io.stderr:write(tostring(reason)..'\n')
      return ec_bump()
    else
      if list() then
        if not options.q then
          io.stderr:write("rmdir: failed to remove " .. path .. ": Directory not empty\n")
        end
        return ec_bump()
      else
        -- path exists and is empty?
        local ok, reason = fs.remove(rpath)
        if not ok then
          io.stderr:write(tostring(reason)..'\n')
          return ec_bump(), reason
        end
        return remove(...) -- the final return of all else
      end
    end
  end
end

for _,path in ipairs(args) do
  -- clean up the input
  path = path:gsub('/+', '/')

  local segments = {}
  if options.p and path:len() > 1 and path:find('/') then
    local chain = text.split(path, {'/'}, true)
    local prefix = ''
    for _,e in ipairs(chain) do
      table.insert(segments, 1, prefix .. e)
      prefix = prefix .. e .. '/'
    end
  else
    segments = {path}
  end

  remove(table.unpack(segments))
end

return ec
