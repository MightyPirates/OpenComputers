local lib = require("transforms")

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

-- Returns a list of subsets of tbl where partitioner acts as a delimiter.
function lib.partition(tbl,partitioner,dropEnds,f,l)
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

function lib.where(tbl,p,f,l)
  return lib.foreach(tbl,
    function(e,i,tbl)
      return p(e,i,tbl)and e or nil
    end,f,l)
end

-- works with pairs on tables
-- returns the kv pair, or nil and the number of pairs iterated
function lib.at(tbl, index)
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
end
