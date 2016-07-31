local fs = require("filesystem")
local shell = require("shell")
local term = require("term")

local dirsArg, ops = shell.parse(...)
if #dirsArg == 0 then
  table.insert(dirsArg, ".")
end

local ec = 0
local gpu = term.gpu()
local fOut = term.isAvailable() and io.output().tty
local function perr(msg) io.stderr:write(msg,"\n") ec = 2 end
local function _path(n,i) return n[i]:sub(1, 1) == '/' and "" or n.path end
local function _name(n,i) return ops.p and n[i] or n[i]:gsub("/+$", "") end
local function _sort_name(n,i) return _name(n,i):gsub("^%.","") end
local function _fullPath(n,i) return fs.concat(_path(n,i),_name(n,i)) end
local function _isLink(n,i) return (fs.isLink(_fullPath(n,i))) end
local function _linkPath(n,i) return select(2,fs.isLink(_fullPath(n,i))) end
local function _isDir(n,i) return fs.isDirectory(_fullPath(n,i)) end
local function _size(n,i) return _isLink(n,i) and 0 or fs.size(_fullPath(n,i)) end
local function _time(n,i) return fs.lastModified(_fullPath(n,i)) end
local function _ext(n,i) return _name(n,i):match("(%.[^.]+)$") or "" end
local function toArray(i) local r={} for n in i do r[#r+1]=n end return r end
local restore_color = function() end
local set_color = function() end
local prev_color
local function colorize(n,i) return prev_color end
if fOut and not ops["no-color"] then
  local LSC = os.getenv("LS_COLORS")
  if type(LSC) == "string" then
    LSC = require("serialization").unserialize(LSC)
  end
  if not LSC then
    perr("ls: unparsable value for LS_COLORS environment variable")
  else
    prev_color = gpu.getForeground()
    restore_color = function() gpu.setForeground(prev_color) end
    colorize=function(n,i) return
      _isLink(n,i) and LSC.LINK or
      _isDir(n,i) and LSC.DIR or
      LSC['*'.._ext(n,i)] or LSC.FILE or prev_color
    end
    set_color=function(c)
      if gpu.getForeground() ~= c then
        io.stdout:flush()
        gpu.setForeground(c)
      end
    end
  end
end
local msft={reports=0,proxies={}}
function msft.report(files, dirs, used, proxy)
  local free = proxy.spaceTotal() - proxy.spaceUsed()
  restore_color()
  local pattern = "%5i File(s) %11i bytes\n%5i Dir(s)  %11s bytes free\n"
  io.write(string.format(pattern, files, used, dirs, tostring(free)))
end
function msft.tail(n)
  local x = fs.get(n.path)
  if not x then return end
  local u,f,d=0,0,0
  for i=1,#n do
    if _isDir(n,i) then d=d+1
    else f=f+1;u=u+_size(n,i) end
  end
  msft.report(f,d,u,x)
  local ps=msft.proxies
  ps[x]=ps[x]or{files=0,dirs=0,used=0}
  local p=ps[x]
  p.files=p.files+f
  p.dirs=p.dirs+d
  p.used=p.used+u
  msft.reports=msft.reports+1
end
function msft.final()
  if msft.reports < 2 then return end
  local g = {}
  for p,r in pairs(msft.proxies) do g[#g+1]={proxy=p,report=r} end
  restore_color()
  print("Total Files Listed:")
  for _,p in ipairs(g) do
    if #g>1 then print("As pertaining to: "..p.proxy.address) end
    msft.report(p.report.files, p.report.dirs, p.report.used, p.proxy)
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
local day_names={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday",
  "Saturday"}
local month_names={"January","February","March","April","May","June","July",
  "August","September","October","November","December"}
local function formatDate(epochms)
  local d = os.date("*t", epochms)
  return ops['full-time'] and
    string.format("%s-%s-%s %s:%s:%s",d.year,pad(nod(d.month)),pad(nod(d.day)),pad(nod(d.hour)),pad(nod(d.min)),pad(nod(d.sec))) or
    string.format("%s %+2s %+2s:%+2s",month_names[d.month]:sub(1,3),nod(d.day),pad(nod(d.hour)),pad(nod(d.min)))
end
local function filter(n)
  if ops.a then return n end
  local s = {path=n.path}
  for i,x in ipairs(n) do
    if fs.name(_name(n,i)):sub(1,1) ~= "." then s[#s+1]=x end
  end
  return s
end
local function sort(n)
  local once=false
  local function ni(v) for i=1,#n do if n[i]==v then return i end end end
  local function sorter(_fp)
    once=true table.sort(n,function(a,b)return _fp(n,ni(a))>_fp(n,ni(b))end)
  end
  local rev = ops.r or ops.reverse
  if ops.t then sorter(_time) end
  if ops.X then sorter(_ext) end
  if ops.S then sorter(_size) end
  if not once then sorter(_sort_name) rev=not rev end
  if rev then
    for i=1,#n/2 do n[i],n[#n-i+1]=n[#n-i+1],n[i] end
  end
  return n
end
local function dig(n, dirs, dir)
  if ops.R then
    local di = 1
    for i=1,#n do
      if _isDir(n,i) then
        local p=dir..(dir:sub(-1) == "/" and "" or "/")
        table.insert(dirs,di,p.._name(n,i))
        di=di+1
      end
    end
  end
  return n
end
local function wide(n,i)
  local t = _isLink(n,i) and 'l' or _isDir(n,i) and 'd' or 'f'
  local link_target = _isLink(n,i) and
    string.format(" -> %s",_linkPath(n,i)..(_isDir(n,i)and"/"or""))or""
  local w = fs.get(_fullPath(n,i)).isReadOnly() and '-' or 'w'
  local size = formatSize(_size(n,i))
  local modDate = formatDate(_time(n,i))
  return string.format("%s-r%s %+7s %s ",t,w,size,modDate),_name(n,i)..link_target
end

local first_display = true
local function display(n)
  local mt={}
  local lines=setmetatable({},mt)
  if ops.l then
    lines.n=#n
    mt.__index=function(tbl,index)local m,l=wide(n,index)return{{color=prev_color,name=m},{color=colorize(n,index),name=l}}end
  elseif ops["1"] or not fOut then
    lines.n=#n
    mt.__index=function(tbl,index)local m,l=wide(n,index)return{{color=colorize(n,index),name=_name(n,index)}}end
  else -- columns
    local cols,d,w=0,0,select(3,term.getGlobalArea())-1
    local function real(x, y)
      local index = y + ((x-1) * d)
      return index <= #n and index or nil
    end
    local function max_name(ci)
      local max=0 -- return the width of the max element in ci
      for r=1,d do
        local ri=real(ci,r)
        if not ri then break end
        max=math.max(max,_name(n,ri):len())
      end
      return max
    end
    local function measure(_cols)
      local t=0
      for c=1,_cols do t=t+max_name(c)+(c>1 and 2 or 0) end
      return t
    end
    while d<#n do d=d+1 cols=math.ceil(#n/d) if measure(cols)<w then break end end
    lines.n=d
    mt.__index=function(tbl,di)return setmetatable({},{
      __len=function()return cols end,
      __index=function(tbl,ci)
        local ri=real(ci, di)
        if not ri then return end
        local nm=_name(n,ri)
        return{
          color=colorize(n,ri),
          name=nm..string.rep(' ',max_name(ci)-#nm+(ci<cols and 2 or 0))
    }end})end
  end
  for li=1,lines.n do
    local l=lines[li]
    for ei=1,#l do
      local e=l[ei]
      if not e then break end
      first_display = false
      set_color(e.color)
      io.write(e.name)
    end
    print()
  end
  msft.tail(n)
end
local header = function() end
if #dirsArg > 1 or ops.R then
  header = function(path)
    if not first_display then print() end
    restore_color()
    io.write(path,":\n")
  end
end
local function splitDirsFromFileArgs(dirs)
  local trimmed = {}
  local files = {}
  for _,dir in ipairs(dirs) do
    local path = shell.resolve(dir)
    if not fs.exists(path) then
      perr("cannot access " .. tostring(path) .. ": No such file or directory")
    elseif fs.isDirectory(path) then
      table.insert(trimmed, dir)
    else -- file or link
      table.insert(files, dir)
    end
  end
  return files, trimmed
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
      local n=toArray(list)
      n.path=path
      display(dig(sort(filter(n)),dirs,dir))
    end
  end
end
local tr,cp={},{path=shell.getWorkingDirectory()}
for _,dir in ipairs(dirsArg) do
  local path = shell.resolve(dir)
  if not fs.exists(path) then
    perr("cannot access " .. tostring(path) .. ": No such file or directory")
  elseif fs.isDirectory(path) then
    tr[#tr+1]=dir
  else -- file or link
    cp[#cp+1]=dir
  end
end
io.output():setvbuf("line")
if #cp > 0 then display(sort(cp)) end
displayDirList(tr)
msft.final()
io.output():flush()
io.output():setvbuf("no")
restore_color()
return ec
