local fs = require("filesystem")
local shell = require("shell")
local tty = require("tty")
local unicode = require("unicode")
local tx = require("transforms")
local text = require("text")

local dirsArg, ops = shell.parse(...)

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
  -M                         display Microsoft-style file and directory
                             count after listing
      --no-color             Do not colorize the output (default colorized)
      --help                 display this help and exit
For more info run: man ls]])
  return 0
end

if #dirsArg == 0 then
  table.insert(dirsArg, ".")
end

local ec = 0
local fOut = tty.isAvailable() and io.output().tty
local function perr(msg) io.stderr:write(msg,"\n") ec = 2 end
local set_color = function() end
local function colorize() return end
if fOut and not ops["no-color"] then
  local LSC = tx.foreach(text.split(os.getenv("LS_COLORS") or "", {":"}, true), function(e)
    local parts = text.split(e, {"="}, true)
    return parts[2], parts[1]
  end)
  colorize = function(info)
    return
      info.isLink and LSC.ln or
      info.isDir and LSC.di or
      LSC['*'..info.ext] or
      LSC.fi
  end
  set_color=function(c)
    io.write(string.char(0x1b), "[", c or "", "m")
  end
end
local msft={reports=0,proxies={}}
function msft.report(files, dirs, used, proxy)
  local free = proxy.spaceTotal() - proxy.spaceUsed()
  set_color()
  local pattern = "%5i File(s) %s bytes\n%5i Dir(s)  %11s bytes free\n"
  io.write(string.format(pattern, files, tostring(used), dirs, tostring(free)))
end
function msft.tail(names)
  local fsproxy = fs.get(names.path)
  if not fsproxy then
    return
  end
  local totalSize, totalFiles, totalDirs = 0, 0, 0
  for i=1,#names do
    local info = names[i]
    if info.isDir then
      totalDirs = totalDirs + 1
    else
      totalFiles = totalFiles + 1
    end
    totalSize = totalSize + info.size
  end
  msft.report(totalFiles, totalDirs, totalSize, fsproxy)
  local ps = msft.proxies
  ps[fsproxy] = ps[fsproxy] or {files=0,dirs=0,used=0}
  local p = ps[fsproxy]
  p.files = p.files + totalFiles
  p.dirs = p.dirs + totalDirs
  p.used = p.used + totalSize
  msft.reports = msft.reports + 1
end
function msft.final()
  if msft.reports < 2 then return end
  local groups = {}
  for proxy,report in pairs(msft.proxies) do
    table.insert(groups, {proxy=proxy,report=report})
  end
  set_color()
  print("Total Files Listed:")
  for _,pair in ipairs(groups) do
    local proxy, report = pair.proxy, pair.report
    if #groups>1 then
      print("As pertaining to: "..proxy.address)
    end
    msft.report(report.files, report.dirs, report.used, proxy)
  end
end

if not ops.M then
  msft.tail=function()end
  msft.final=function()end
end

local function nod(n)
  return n and (tostring(n):gsub("(%.[0-9]+)0+$","%1")) or "0"
end

local function formatSize(size)
  if not ops.h and not ops['human-readable'] and not ops.si then
    return tostring(size)
  end
  local sizes = {"", "K", "M", "G"}
  local unit = 1
  local power = ops.si and 1000 or 1024
  while size > power and unit < #sizes do
    unit = unit + 1
    size = size / power
  end
  return nod(math.floor(size*10)/10)..sizes[unit]
end

local function pad(txt)
  txt = tostring(txt)
  return #txt >= 2 and txt or "0"..txt
end

local function formatDate(epochms)
  --local day_names={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"}
  local month_names={"January","February","March","April","May","June","July","August","September","October","November","December"}
  if epochms == 0 then return "" end
  local d = os.date("*t", epochms)
  local day, hour, min, sec = nod(d.day), pad(nod(d.hour)), pad(nod(d.min)), pad(nod(d.sec))
  if ops["full-time"] then
    return string.format("%s-%s-%s %s:%s:%s ", d.year, pad(nod(d.month)), pad(day), hour, min, sec)
  else
    return string.format("%s %2s %2s:%2s ", month_names[d.month]:sub(1,3), day, hour, pad(min))
  end
end

local function filter(names)
  if ops.a then
    return names
  end
  local set = { path = names.path }
  for _, info in ipairs(names) do
    if fs.name(info.name):sub(1, 1) ~= "." then
      set[#set + 1] = info
    end
  end
  return set
end

local function sort(names)
  local once = false
  local function ni(v)
    for i=1,#names do
      local info = names[i]
      if info.key == v.key then
        return i
      end
    end
  end
  local function sorter(key)
    once = true
    table.sort(names, function(a, b)
      local ast = names[ni(a)]
      local bst = names[ni(b)]
      return ast[key] > bst[key]
    end)
  end
  if ops.t then sorter("time") end
  if ops.X then sorter("ext") end
  if ops.S then sorter("size") end
  local rev = ops.r or ops.reverse
  if not once then sorter("sort_name") rev=not rev end
  if rev then
    for i=1,#names/2 do
      names[i], names[#names - i + 1] = names[#names - i + 1], names[i]
    end
  end
  return names
end

local function dig(names, dirs, dir)
  if ops.R then
    local di = 1
    for i=1,#names do
      local info = names[i]
      if info.isDir then
        local path = dir..(dir:sub(-1) == "/" and "" or "/")
        table.insert(dirs, di, path..info.name)
        di = di + 1
      end
    end
  end
  return names
end

local first_display = true
local function display(names)
  local mt={}
  local lines = setmetatable({}, mt)
  if ops.l then
    lines.n = #names
    local max_size_width = 1
    local max_date_width = 0
    for i=1,lines.n do
      local info = names[i]
      max_size_width = math.max(max_size_width, formatSize(info.size):len())
      max_date_width = math.max(max_date_width, formatDate(info.time):len())
    end
    mt.__index = function(_, index)
      local info = names[index]
      local file_type = info.isLink and 'l' or info.isDir and 'd' or 'f'
      local link_target = info.isLink and string.format(" -> %s", info.link:gsub("/+$", "") .. (info.isDir and "/" or "")) or ""
      local write_mode = info.fs.isReadOnly() and '-' or 'w'
      local size = formatSize(info.size)
      local modDate = formatDate(info.time)
      local format = "%s-r%s %"..tostring(max_size_width).."s %"..tostring(max_date_width).."s"
      local meta = string.format(format, file_type, write_mode, size, modDate)
      local item = info.name..link_target
      return {{name = meta}, {color = colorize(info), name = item}}
    end
  elseif ops["1"] or not fOut then
    lines.n = #names
    mt.__index = function(_, index)
      local info = names[index]
      return {{color = colorize(info), name = info.name}}
    end
  else -- columns
    local num_columns, items_per_column, width = 0, 0, tty.getViewport() - 1
    local function real(x, y)
      local index = y + ((x-1) * items_per_column)
      return index <= #names and index or nil
    end
    local function max_name(column_index)
      local max = 0 -- return the width of the max element in column_index
      for r=1,items_per_column do
        local ri = real(column_index, r)
        if not ri then break end
        local info = names[ri]
        max = math.max(max, unicode.wlen(info.name))
      end
      return max
    end
    local function measure()
      local total = 0
      for column_index=1,num_columns do
        total = total + max_name(column_index) + (column_index > 1 and 2 or 0)
      end
      return total
    end
    while items_per_column<#names do
      items_per_column = items_per_column + 1
      num_columns = math.ceil(#names/items_per_column)
      if measure() < width then
        break
      end
    end
    lines.n = items_per_column
    mt.__index=function(_, line_index)
      return setmetatable({},{
        __len=function()return num_columns end,
        __index=function(_, column_index)
          local ri = real(column_index, line_index)
          if not ri then return end
          local info = names[ri]
          local name = info.name
          return {color = colorize(info), name = name .. string.rep(' ', max_name(column_index) - unicode.wlen(name) + (column_index < num_columns and 2 or 0))}
        end,
      })
    end
  end
  for line_index=1,lines.n do
    local line = lines[line_index]
    for element_index=1,#line do
      local e = line[element_index]
      if not e then break end
      first_display = false
      set_color(e.color)
      io.write(e.name)
    end
    set_color()
    print()
  end
  msft.tail(names)
end
local header = function() end
if #dirsArg > 1 or ops.R then
  header = function(path)
    if not first_display then print() end
    set_color()
    io.write(path,":\n")
  end
end

local function stat(path, name)
  local info = {}
  info.key = name
  info.path = name:sub(1, 1) == "/" and "" or path
  info.full_path = fs.concat(info.path, name)
  info.isDir = fs.isDirectory(info.full_path)
  info.name = name:gsub("/+$", "") .. (ops.p and info.isDir and "/" or "")
  info.sort_name = info.name:gsub("^%.","")
  info.isLink, info.link = fs.isLink(info.full_path)
  info.size = info.isLink and 0 or fs.size(info.full_path)
  info.time = fs.lastModified(info.full_path)/1000
  info.fs = fs.get(info.full_path)
  info.ext = info.name:match("(%.[^.]+)$") or ""
  return info
end

local function displayDirList(dirs)
  while #dirs > 0 do
    local dir = table.remove(dirs, 1)
    header(dir)
    local path = shell.resolve(dir)
    local list, reason = fs.list(path)
    if not list then
      perr(reason)
    else
      local names = { path = path }
      for name in list do
        names[#names + 1] = stat(path, name)
      end
      display(dig(sort(filter(names)), dirs, dir))
    end
  end
end
local dir_set, file_set = {}, {path=shell.getWorkingDirectory()}
for _,dir in ipairs(dirsArg) do
  local path = shell.resolve(dir)
  local real, why = fs.realPath(path)
  local access_msg = "cannot access " .. tostring(path) .. ": "
  if not real then
    perr(access_msg .. why)
  elseif not fs.exists(path) then
    perr(access_msg .. "No such file or directory")
  elseif fs.isDirectory(path) then
    table.insert(dir_set, dir)
  else -- file or link
    table.insert(file_set, stat(dir, dir))
  end
end

io.output():setvbuf("line")

local ok, msg = pcall(function()
  if #file_set > 0 then display(sort(file_set)) end
  displayDirList(dir_set)
  msft.final()
end)

io.output():flush()
io.output():setvbuf("no")

assert(ok, msg)

return ec

