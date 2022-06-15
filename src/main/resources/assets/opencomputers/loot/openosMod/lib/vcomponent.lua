local component = require("component")
local computer = require("computer")

local proxylist = {}
local proxyobjs = {}
local typelist = {}
local doclist = {}

local oproxy = component.proxy
function component.proxy(address)
  checkArg(1,address,"string")
  if proxyobjs[address] ~= nil then
    return proxyobjs[address]
  end
  return oproxy(address)
end

local olist = component.list
function component.list(filter, exact)
  checkArg(1,filter,"string","nil")
  local result = {}
  local data = {}
  for k,v in olist(filter, exact) do
    data[#data + 1] = k
    data[#data + 1] = v
    result[k] = v
  end
  for k,v in pairs(typelist) do
    if filter == nil or (exact and v == filter) or (not exact and v:find(filter, nil, true)) then
      data[#data + 1] = k
      data[#data + 1] = v
      result[k] = v
    end
  end
  local place = 1
  return setmetatable(result, 
    {__call=function()
      local addr,type = data[place], data[place + 1]
      place = place + 2
      return addr, type
    end}
  )
end

local otype = component.type
function component.type(address)
  checkArg(1,address,"string")
  if typelist[address] ~= nil then
    return typelist[address]
  end
  return otype(address)
end

local odoc = component.doc
function component.doc(address, method)
  checkArg(1,address,"string")
  checkArg(2,method,"string")
  if proxylist[address] ~= nil then
    if proxylist[address][method] == nil then
      error("no such method",2)
    end
    if doclist[address] ~= nil then
      return doclist[address][method]
    end
    return nil
  end
  return odoc(address, method)
end

local oslot = component.slot
function component.slot(address)
  checkArg(1,address,"string")
  if proxylist[address] ~= nil then
    return -1 -- vcomponents do not exist in a slot
  end
  return oslot(address)
end

local omethods = component.methods
function component.methods(address)
  checkArg(1,address,"string")
  if proxylist[address] ~= nil then
    local methods = {}
    for k,v in pairs(proxylist[address]) do
      if type(v) == "function" then
        methods[k] = true -- All vcomponent methods are direct
      end
    end
    return methods
  end
  return omethods(address)
end

local oinvoke = component.invoke
function component.invoke(address, method, ...)
  checkArg(1,address,"string")
  checkArg(2,method,"string")
  if proxylist[address] ~= nil then
    if proxylist[address][method] == nil then
      error("no such method",2)
    end
    return proxylist[address][method](...)
  end
  return oinvoke(address, method, ...)
end

local ofields = component.fields
function component.fields(address)
  checkArg(1,address,"string")
  if proxylist[address] ~= nil then
    return {} -- What even is this?
  end
  return ofields(address)
end

local componentCallback =
{
  __call = function(self, ...) return proxylist[self.address][self.name](...) end,
  __tostring = function(self) return (doclist[self.address] ~= nil and doclist[self.address][self.name] ~= nil) and doclist[self.address][self.name] or "function" end
}

local vcomponent = {}

function vcomponent.register(address, ctype, proxy, doc)
  checkArg(1,address,"string")
  checkArg(2,ctype,"string")
  checkArg(3,proxy,"table")
  if proxylist[address] ~= nil then
    return nil, "component already at address"
  elseif component.type(address) ~= nil then
    return nil, "cannot register over real component"
  end
  proxy.address = address
  proxy.type = ctype
  local proxyobj = {}
  for k,v in pairs(proxy) do
    if type(v) == "function" then
      proxyobj[k] = setmetatable({name=k,address=address},componentCallback)
    else
      proxyobj[k] = v
    end
  end
  proxylist[address] = proxy
  proxyobjs[address] = proxyobj
  typelist[address] = ctype
  doclist[address] = doc
  computer.pushSignal("component_added",address,ctype)
  return true
end

function vcomponent.unregister(address)
  checkArg(1,address,"string")
  if proxylist[address] == nil then
    if component.type(address) ~= nil then
      return nil, "cannot unregister real component"
    else
      return nil, "no component at address"
    end
  end
  local thetype = typelist[address]
  proxylist[address] = nil
  proxyobjs[address] = nil
  typelist[address] = nil
  doclist[address] = nil
  computer.pushSignal("component_removed",address,thetype)
  return true
end

function vcomponent.list()
  local list = {}
  for k,v in pairs(proxylist) do
    list[#list + 1] = {k,typelist[k],v}
  end
  return list
end

function vcomponent.resolve(address, componentType)
  checkArg(1, address, "string")
  checkArg(2, componentType, "string", "nil")
  for k,v in pairs(typelist) do
    if componentType == nil or v == componentType then
      if k:sub(1, #address) == address then
        return k
      end
    end
  end
  return nil, "no such component"
end

local r = math.random
function vcomponent.uuid()
  return string.format("%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
  r(0,255),r(0,255),r(0,255),r(0,255),
  r(0,255),r(0,255),
  r(64,79),r(0,255),
  r(128,191),r(0,255),
  r(0,255),r(0,255),r(0,255),r(0,255),r(0,255),r(0,255))
end

return vcomponent
