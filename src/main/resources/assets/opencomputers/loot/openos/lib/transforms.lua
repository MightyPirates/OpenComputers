local lib={}
lib.internal={}
function lib.internal.range_adjust(f,l,s)
  checkArg(1,f,'number','nil')
  checkArg(2,l,'number','nil')
  checkArg(3,s,'number')
  if f==nil then f=1 elseif f<0 then f=s+f+1 end
  if l==nil then l=s elseif l<0 then l=s+l+1 end
  return f,l
end
function lib.internal.table_view(tbl,f,l)
  return setmetatable({},
  {
    __index = function(_, key)
      return (type(key) ~= 'number' or (key >= f and key <= l)) and tbl[key] or nil
    end,
    __len = function(_)
      return l
    end,
  })
end
local adjust=lib.internal.range_adjust
local view=lib.internal.table_view

-- first(p1,p2) searches for the first range in p1 that satisfies p2
function lib.first(tbl,pred,f,l)
  checkArg(1,tbl,'table')
  checkArg(2,pred,'function','table')
  if type(pred)=='table'then
    local set;set,pred=pred,function(e,fi,tbl)
      for vi=1,#set do
        local v=set[vi]
        if lib.begins(tbl,v,fi) then return true,#v end
      end
    end
  end
  local s=#tbl
  f,l=adjust(f,l,s)
  tbl=view(tbl,f,l)
  for i=f,l do
    local si,ei=pred(tbl[i],i,tbl)
    if si then
      return i,i+(ei or 1)-1
    end
  end
end

-- returns true if p1 at first p3 equals element for element p2
function lib.begins(tbl,v,f,l)
  checkArg(1,tbl,'table')
  checkArg(2,v,'table')
  local vs=#v
  f,l=adjust(f,l,#tbl)
  if vs>(l-f+1)then return end
  for i=1,vs do
    if tbl[f+i-1]~=v[i] then return end
  end
  return true
end

function lib.concat(...)
  local r,rn,k={},0
  for _,tbl in ipairs({...})do
    if type(tbl)~='table'then
      return nil,'parameter '..tostring(_)..' to concat is not a table'
    end
    local n=tbl.n or #tbl
    k=k or tbl.n
    for i=1,n do
      rn=rn+1;r[rn]=tbl[i]
    end
  end
  r.n=k and rn or nil
  return r
end

require("package").delay(lib, "/lib/core/full_transforms.lua")

return lib
