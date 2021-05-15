local shell = require("shell")
local fs = require("filesystem")

local args, options, reason = shell.parse(...)
if #args == 0 then
  args[1] = '.'
end

local TRY=[[
Try 'du --help' for more information.]]

local VERSION=[[
du (OpenOS bin) 1.0
Written by payonel, patterned after GNU coreutils du]]

local HELP=[[
Usage: du [OPTION]... [FILE]...
Summarize disk usage of each FILE, recursively for directories.

  -h, --human-readable  print sizes in human readable format (e.g., 1K 234M 2G)
  -s, --summarize       display only a total for each argument
      --help     display this help and exit
      --version  output version information and exit]]

if options.help then
  print(HELP)
  return true
end

if options.version then
  print(VERSION)
  return true
end

local function addTrailingSlash(path)
  if path:sub(-1) ~= '/' then
    return path .. '/'
  else
    return path
  end
end

local function opCheck(shortName, longName)
  local enabled = options[shortName] or options[longName]
  options[shortName] = nil
  options[longName] = nil
  return enabled
end

local bHuman = opCheck('h', 'human-readable')
local bSummary = opCheck('s', 'summarize')

if next(options) then
  for op,v in pairs(options) do
    io.stderr:write(string.format("du: invalid option -- '%s'\n", op))
  end
  io.stderr:write(TRY..'\n')
  return 1
end

local function formatSize(size)
  if not bHuman then
      return tostring(size)
  end
  local sizes = {"", "K", "M", "G"}
  local unit = 1
  local power = options.si and 1000 or 1024
  while size > power and unit < #sizes do
    unit = unit + 1
    size = size / power
  end

  return math.floor(size * 10) / 10 .. sizes[unit]
end

local function printSize(size, rpath)
  local displaySize = formatSize(size)
  io.write(string.format("%s%s\n", string.format("%-12s", displaySize), rpath))
end

local function visitor(rpath)
  local subtotal = 0
  local dirs = 0
  local spath = shell.resolve(rpath)

  if fs.isDirectory(spath) then
    local list_result = fs.list(spath)
    for list_item in list_result do
      local vtotal, vdirs = visitor(addTrailingSlash(rpath) .. list_item)
      subtotal = subtotal + vtotal
      dirs = dirs + vdirs
    end

    if dirs == 0 then -- no child dirs
      if not bSummary then
        printSize(subtotal, rpath)
      end
    end

  elseif not fs.isLink(spath) then
    subtotal = fs.size(spath)
  end

  return subtotal, dirs
end

for i,arg in ipairs(args) do
  local path = shell.resolve(arg)

  if not fs.exists(path) then
    io.stderr:write(string.format("du: cannot access '%s': no such file or directory\n", arg))
    return 1
  else
    if fs.isDirectory(path) then
      local total = visitor(arg)

      if bSummary then
        printSize(total, arg)
      end
    elseif fs.isLink(path) then
      printSize(0, arg)
    else
      printSize(fs.size(path), arg)
    end
  end
end

return true
