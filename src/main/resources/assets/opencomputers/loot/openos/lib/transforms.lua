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
    __index=function(b,k)return(type(k)~='number'or(k>=f and k<=l))and tbl[k]or nil end,
    __len=function(b)return#tbl end,
  })
end
local adjust=lib.internal.range_adjust
local view=lib.internal.table_view

-- works like string.sub but on elements of an indexed table
function lib.sub(tbl,f,l)
  checkArg(1,tbl,'table')
  local r,s={},#tbl
  f,l=adjust(f,l,s)
  l=math.min(l,s)
  for i=math.max(f,1),l do
    r[#r+1]=tbl[i]
  end
  return r
end

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

-- if value was made by lib.sub then find can find from whence
function --[[@delayloaded-start@]] lib.find(tbl, sub, first, last)
  checkArg(1, tbl, 'table')
  checkArg(2, sub, 'table')
  local sub_len = #sub
  return lib.first(tbl, function(element, index, projected_table)
    for n=0,sub_len-1 do
      if projected_table[n + index] ~= sub[n + 1] then return nil end
    end
    return 1, sub_len
  end, first, last)
end --[[@delayloaded-end@]] 

-- Returns a list of subsets of tbl where partitioner acts as a delimiter.
function --[[@delayloaded-start@]] lib.partition(tbl,partitioner,dropEnds,f,l)
  checkArg(1,tbl,'table')
  checkArg(2,partitioner,'function','table')
  checkArg(3,dropEnds,'boolean','nil')
  if type(partitioner)=='table'then
    return lib.partition(tbl,function(e,i,tbl)
      return lib.first(tbl,partitioner,i)
    end,dropEnds,f,l)
  end
  local s=#tbl
  f,l=adjust(f,l,s)
  local cut=view(tbl,f,l)
  local result={}
  local need=true
  local exp=function()if need then result[#result+1]={}need=false end end
  local i=f
  while i<=l do
    local e=cut[i]
    local ds,de=partitioner(e,i,cut)
    -- true==partition here
    if ds==true then ds,de=i,i
    elseif ds==false then ds,de=nil,nil end
    if ds~=nil then
      ds,de=adjust(ds,de,l)
      ds=ds>=i and ds--no more
    end
    if not ds then -- false or nil
      exp()
      table.insert(result[#result],e)
    else
      local sub=lib.sub(cut,i,not dropEnds and de or (ds-1))
      if #sub>0 then
        exp()
        result[#result+math.min(#result[#result],1)]=sub
      end
      -- ensure i moves forward
      local ensured=math.max(math.max(de or ds,ds),i)
      if de and ds and de<ds and ensured==i then
        if #result==0 then result[1]={} end
        table.insert(result[#result],e)
      end
      i=ensured
      need=true
    end
    i=i+1
  end

  return result
end --[[@delayloaded-end@]] 

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

-- calls callback(e,i,tbl) for each ith element e in table tbl from first
function lib.foreach(tbl,c,f,l)
  checkArg(1,tbl,'table')
  checkArg(2,c,'function','string')
  local ck=c
  c=type(c)=="string" and function(e) return e[ck] end or c
  local s=#tbl
  f,l=adjust(f,l,s)
  tbl=view(tbl,f,l)
  local r={}
  for i=f,l do
    local n,k=c(tbl[i],i,tbl)
    if n~=nil then
      if k then r[k]=n
      else r[#r+1]=n end
    end
  end
  return r
end
lib.select=lib.foreach

function  --[[@delayloaded-start@]] lib.where(tbl,p,f,l)
  return lib.foreach(tbl,
    function(e,i,tbl)
      return p(e,i,tbl)and e or nil
    end,f,l)
end --[[@delayloaded-end@]] 

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

-- works with pairs on tables
-- returns the kv pair, or nil and the number of pairs iterated
function  --[[@delayloaded-start@]] lib.at(tbl, index)
  checkArg(1, tbl, "table")
  checkArg(2, index, "number", "nil")
  local current_index = 1
  for k,v in pairs(tbl) do
    if current_index == index then
      return k,v
    end
    current_index = current_index + 1
  end
  return nil, current_index - 1 -- went one too far
end --[[@delayloaded-end@]] 

return lib,{adjust=adjust,view=view}
