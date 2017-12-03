local computer = require("computer")
local shell = require("shell")
local fs = require("filesystem")
local tx = require("transforms")
local text = require("text")

local args, opts = shell.parse(...)

local function die(...)
  io.stderr:write(...)
  os.exit(1)
end

do -- handle cli
  if opts.help then
    print([[Usage: tree [OPTION]... [FILE]...
  -a, --all             do not ignore entries starting with .
      --full-time       with -l, print time in full iso format
  -h, --human-readable  with -l, print human readable sizes
      --si              likewise, but use powers of 1000 not 1024
      --level=LEVEL     descend only LEVEL directories deep
      --color=WHEN      WHEN can be
                        auto - colorize output only if writing to a tty,
                        always - always colorize output,
                        never - never colorize output; (default: auto)
  -l                    use a long listing format
  -f                    print the full path prefix for each file
  -i                    do not print indentation lines
  -p                    append "/" indicator to directories
  -Q, --quote           quote filenames with double quotes
  -r, --reverse         reverse order while sorting
  -S                    sort by file size
  -t                    sort by modification type, newest first
  -X                    sort alphabetically by entry extension
  -C                    do not count files and directories
  -R                    count root directories like other files
      --help            print this help and exit]])
    return 0
  end

  if #args == 0 then
    table.insert(args, ".")
  end

  opts.level = tonumber(opts.level) or math.huge
  if opts.level < 1 then
    die("Invalid level, must be greater than 0")
  end

  opts.color = opts.color or "auto"
  if opts.color == "auto" then
    opts.color = io.stdout.tty and "always" or "never"
  end

  if opts.color ~= "always" and opts.color ~= "never" then
    die("Invalid value for --color=WHEN option; WHEN should be auto, always or never")
  end
end

local lastYield = computer.uptime()
local function yieldopt()
  if computer.uptime() - lastYield > 2 then
    lastYield = computer.uptime()
    os.sleep(0)
  end
end

local function peekable(iterator, state, var1)
  local nextItem = {iterator(state, var1)}

  return setmetatable({
    peek = function()
      return table.unpack(nextItem)
    end
  }, {
    __call = coroutine.wrap(function()
      while true do
        local item = nextItem
        nextItem = {iterator(state, nextItem[1])}
        coroutine.yield(table.unpack(item))
        if nextItem[1] == nil then break end
      end
    end)
  })
end

local function filter(entry)
  return opts.a or entry:sub(1, 1) ~= "."
end

local function stat(path)
  local st = {}
  st.path = path
  st.name = fs.name(path) or "/"
  st.sortName = st.name:gsub("^%.","")
  st.time = fs.lastModified(path)
  st.isLink = fs.isLink(path)
  st.isDirectory = fs.isDirectory(path)
  st.size = st.isLink and 0 or fs.size(path)
  st.extension = st.name:match("(%.[^.]+)$") or ""
  st.fs = fs.get(path)
  return st
end

local colorize
if opts.color == "always" then
  -- from /lib/core/full_ls.lua
  local colors = tx.foreach(text.split(os.getenv("LS_COLORS") or "", {":"}, true), function(e)
    local parts = text.split(e, {"="}, true)
    return parts[2], parts[1]
  end)

  function colorize(stat)
    return stat.isLink and colors.ln or
           stat.isDirectory and colors.di or
           colors["*" .. stat.extension] or
           colors.fi
  end
end

local function list(path)
  return coroutine.wrap(function()
    local l = {}
    for entry in fs.list(path) do
      if filter(entry) then
        table.insert(l, stat(fs.concat(path, entry)))
      end
    end

    if opts.S then
      table.sort(l, function(a, b)
        return a.size < b.size
      end)
    elseif opts.t then
      table.sort(l, function(a, b)
        return a.time < b.time
      end)
    elseif opts.X then
      table.sort(l, function(a, b)
        return a.extension < b.extension
      end)
    else
      table.sort(l, function(a, b)
        return a.sortName < b.sortName
      end)
    end

    for i = opts.r and #l or 1, opts.r and 1 or #l, opts.r and -1 or 1 do
      coroutine.yield(l[i])
    end
  end)
end

local function digRoot(rootPath)
  coroutine.yield(stat(rootPath), {})

  if not fs.isDirectory(rootPath) then
    return
  end
  local iterStack = {peekable(list(rootPath))}
  local pathStack = {rootPath}
  local levelStack = {not not iterStack[#iterStack]:peek()}


  repeat
    local entry = iterStack[#iterStack]()

    if entry then
      levelStack[#levelStack] = not not iterStack[#iterStack]:peek()

      local path = fs.concat(fs.concat(table.unpack(pathStack)), entry.name)

      coroutine.yield(entry, levelStack)

      if entry.isDirectory and opts.level > #levelStack then
        table.insert(iterStack, peekable(list(path)))
        table.insert(pathStack, entry.name)
        table.insert(levelStack, not not iterStack[#iterStack]:peek())
      end
    else
      table.remove(iterStack)
      table.remove(pathStack)
      table.remove(levelStack)
    end
  until #iterStack == 0
end

local function dig(roots)
  return coroutine.wrap(function()
    for _, root in ipairs(roots) do
      digRoot(root)
    end
  end)
end

local function nod(n)  -- from /lib/core/full_ls.lua
  return n and (tostring(n):gsub("(%.[0-9]+)0+$","%1")) or "0"
end

local function formatFSize(size)  -- from /lib/core/full_ls.lua
  if not opts.h and not opts["human-readable"] and not opts.si then
    return tostring(size)
  end

  local sizes = {"", "K", "M", "G"}
  local unit = 1
  local power = opts.si and 1000 or 1024

  while size > power and unit < #sizes do
    unit = unit + 1
    size = size / power
  end

  return nod(math.floor(size*10)/10)..sizes[unit]
end

local function pad(txt)  -- from /lib/core/full_ls.lua
  txt = tostring(txt)
  return #txt >= 2 and txt or "0" .. txt
end

local function formatTime(epochms)  -- from /lib/core/full_ls.lua
  local month_names = {"January","February","March","April","May","June",
     "July","August","September","October","November","December"}

  if epochms == 0 then return "" end

  local d = os.date("*t", epochms)
  local day, hour, min, sec = nod(d.day), pad(nod(d.hour)), pad(nod(d.min)), pad(nod(d.sec))

  if opts["full-time"] then
    return string.format("%s-%s-%s %s:%s:%s ", d.year, pad(nod(d.month)), pad(day), hour, min, sec)
  else
    return string.format("%s %+2s %+2s:%+2s ", month_names[d.month]:sub(1,3), day, hour, pad(min))
  end
end

local function writeEntry(entry, levelStack)
  for i, hasNext in ipairs(levelStack) do
    if opts.i then break end

    if i == #levelStack then
      if hasNext then
        io.write("├── ")
      else
        io.write("└── ")
      end
    else
      if hasNext then
        io.write("│   ")
      else
        io.write("    ")
      end
    end
  end

  if opts.l then
    io.write("[")

    io.write(entry.isDirectory and "d" or entry.isLink and "l" or "f", "-")
    io.write("r", entry.fs.isReadOnly() and "-" or "w", " ")

    io.write(formatFSize(entry.size), " ")

    io.write(formatTime(entry.time))
    io.write("] ")
  end

  if opts.Q then io.write('"') end

  if opts.color == "always" then
    io.write("\27[" .. colorize(entry) .. "m")
  end

  if opts.f then
    io.write(entry.path)
  else
    io.write(entry.name)
  end

  if opts.color == "always" then
    io.write("\27[0m")
  end

  if opts.p and entry.isDirectory then
    io.write("/")
  end

  if opts.Q then io.write('"') end
  io.write("\n")
end

local function writeCount(dirs, files)
  io.write("\n")
  io.write(dirs, " director", dirs == 1 and "y" or "ies")
  io.write(", ")
  io.write(files, " file", files == 1 and "" or "s")
  io.write("\n")
end

local dirs, files = 0, 0

local roots = {}
for _, arg in ipairs(args) do
  local path = shell.resolve(arg)
  local real, reason = fs.realPath(path)
  if not real then
    die("cannot access ", path, ": ", reason or "unknown error")
  elseif not fs.exists(path) then
    die("cannot access ", path, ":", "No such file or directory")
  else
    table.insert(roots, real)
  end
end

for entry, levelStack in dig(roots) do
  if opts.R or #levelStack > 0 then
    if entry.isDirectory then
      dirs = dirs + 1
    else
      files = files + 1
    end
  end
  writeEntry(entry, levelStack)
  yieldopt()
end

if not opts.C then
  writeCount(dirs, files)
end

