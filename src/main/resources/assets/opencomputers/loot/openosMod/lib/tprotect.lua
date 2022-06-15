--взято с форума computer craft - https://computercraft.ru/topic/2518-zaschitnik-tablits-tprotect/?tab=comments#comment-37169
--надеюсь штучька надежная так как нужна она мне для сирьезных вешей

local tprotect={} 
local raw_rawset=rawset -- Сохраняем rawset для дальнейшего пользования
local raw_rawget=rawget -- Сохраняем rawget для дальнейшего пользования
local getmetatable=getmetatable --
local setmetatable=setmetatable --
local type=type                 -- Дополнительная зашыта
local error=error               --
local assert=assert             --
local protectid={}
local nextid={}
function rawget(t,k)
  if type(t)=="table" and raw_rawget(t,protectid) then
    error("СЕРЬЁЗНАЯ ПРОБЛЕМА БЕЗОПАСНОСТИ ДЕТЕКТЕД. УНИЧТОЖАЕМ ОПАСНОСТЬ...",2)
  end
  return raw_rawget(t,k)
end
local raw_next=next
-- НИКТО НЕ ДОЛЖЕН УЗНАТЬ МАСТЕР-КЛЮЧ!!!
function next(t,k)
  if type(t)=="table" and raw_rawget(t,protectid) then
    error("СЕРЬЁЗНАЯ ПРОБЛЕМА БЕЗОПАСНОСТИ ДЕТЕКТЕД. УНИЧТОЖАЕМ ОПАСНОСТЬ...",2)
  end
  local ok,k,v=xpcall(raw_next,debug.traceback,t,k)
  if not ok then
    error(k,0)
  end
  return k,v
end
local raw_ipairs=ipairs
function ipairs(...)
  local f,t,z=raw_ipairs(...)
  return function(t,k)
    if type(t)=="table" and raw_rawget(t,protectid) then
      error("СЕРЬЁЗНАЯ ПРОБЛЕМА БЕЗОПАСНОСТИ ДЕТЕКТЕД. УНИЧТОЖАЕМ ОПАСНОСТЬ...",2)
    end
    return f(t,k)
  end,t,z
end
function rawset(t,k,v) -- Потому что в защитные копии таблиц можно было бы записывать. Хоть это бы и не отразилось бы на оригинале, но при попытке индекснуть поле защитной копии будет подложено подмененное поле в обход __index :(
  if k==protectid then
    error("СЕРЬЁЗНАЯ ПРОБЛЕМА БЕЗОПАСНОСТИ ДЕТЕКТЕД. УНИЧТОЖАЕМ ОПАСНОСТЬ...",2)
  end
  assert(type(t)=="table","bad argument #1 to rawset (table expected, got "..type(t)..")")
  assert(type(k)~="nil","bad argument #2 to rawset (table index is nil)")
  local mt=getmetatable(t)
  local no_set=raw_rawget(t,protectid) or (type(mt)=="table" and raw_rawget(mt,protectid))
  if no_set then
    error("таблица рид-онли! Аксес дэняйд!",2)
  end
  raw_rawset(t,k,v)
  return t
end
function tprotect.protect(t)
  local tcopy={[protectid]=true}
  local mto=getmetatable(t)
  local tcopy_mt=type(mto)=="table" and mto or {}
  local mt={[protectid]=true}
  function mt:__index(k)
    local x=t[k]
    if tcopy_mt.__index and not x then
      return tcopy_mt.__index(t,k)
    end
    return t[k]
  end
  function mt:__pairs(self)
    if tcopy_mt.__pairs then
      return tcopy_mt.__pairs(t)
    end
    local function iter(x,i)
      assert(x==self)
      return next(t,i)
    end
    return iter,self,nil
  end
  function mt:__ipairs(self)
    if tcopy_mt.__ipairs then
      return tcopy_mt.__ipairs(t)
    end
    local f,x,i=ipairs(self)
    local function iter(self,i)
      return f(t,i)
    end
    return iter,x,i
  end
  function mt:__newindex(k,v)
    if tcopy_mt.__newindex then -- Мы доверяем нашим клиентам!
      return tcopy_mt.__newindex(self,k,v)
    end
    error("СРЕДНЕНЬКАЯ ПРОБЛЕМА БЕЗОПАСНОСТИ ДЕТЕКТЕД. УНИЧТОЖАЕМ ОПАСНОСТЬ...",2)
  end
  mt.__metatable={"Хочешь проблем? Попытайся взломать tprotect!"}
  setmetatable(mt,{__index=function(self,i)
    local v=tcopy_mt
    if type(v)=="function" then
      return function(self,...)
        return v(t,...)
      end
    end
    return v
  end})
  setmetatable(tcopy,mt)
  return tcopy,tcopy_mt
end
local tprotect_t,tprotect_mt=tprotect.protect(tprotect) -- Защитим нашу библиотечку
return tprotect_t