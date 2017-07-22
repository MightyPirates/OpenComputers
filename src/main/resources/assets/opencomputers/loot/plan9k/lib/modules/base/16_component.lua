local rawComponent = kernel._K.component
component = {}

kernel.userspace.component = component
kernel._K.component = component

kernelGroup = {
    adding = {},
    removing = {},
    primaries = {},
}

local function getGroup()
    return kernel.modules.threading.currentThread and kernel.modules.threading.currentThread.cgroups.component or kernelGroup
end

local function allow(addr)
    if not kernel.modules.threading then return true end
    return not kernel.modules.threading.currentThread or kernel.modules.threading.currentThread.cgroups.component.allow(addr)
end

component.doc = function(addr, method)
    if allow(addr) then
        return rawComponent.doc(addr, method)
    else
        error("no such component")
    end
end

component.invoke = function(addr, ...)
    if allow(addr) then
        return rawComponent.invoke(addr, ...)
    else
        error("no such component")
    end
end

component.list = function(filter, exact)
    local list = {}
    for k, v in pairs(rawComponent.list(filter, not not exact)) do
        if allow(k) then
            list[k] = v
        end
    end
    local key = nil
    return setmetatable(list, {__call=function()
        key = next(list, key)
        if key then
            return key, list[key]
        end
    end})
end

component.methods = function(addr)
    if allow(addr) then
        return rawComponent.methods(addr)
    else
        return nil, "no such component"
    end
end

component.fields = function(addr)
    if allow(addr) then
        return rawComponent.fields(addr)
    else
        return nil, "no such component"
    end
end

component.proxy = function(addr)
    if allow(addr) then
        return rawComponent.proxy(addr)
    else
        return nil, "no such component"
    end
end

component.type = function(addr)
    if allow(addr) then
        return rawComponent.type(addr)
    else
        return nil, "no such component"
    end
end

component.slot = function(addr)
    if allow(addr) then
        return rawComponent.slot(addr)
    else
        return nil, "no such component"
    end
end

--local adding = {}
--local removing = {}
--local primaries = {}

-------------------------------------------------------------------------------

-- This allows writing component.modem.open(123) instead of writing
-- component.getPrimary("modem").open(123), which may be nicer to read.
setmetatable(component, {
  __index = function(_, key)
    return component.getPrimary(key)
  end,
  __pairs = function(self)
    local parent = false
    return function(_, key)
      if parent then
        return next(getGroup().primaries, key)
      else
        local k, v = next(self, key)
        if not k then
          parent = true
          return next(getGroup().primaries)
        else
          return k, v
        end
      end
    end
  end
})

function component.get(address, componentType)
  checkArg(1, address, "string")
  checkArg(2, componentType, "string", "nil")
  for c in component.list(componentType, true) do
    if c:sub(1, address:len()) == address then
      return c
    end
  end
  return nil, "no such component"
end

function component.isAvailable(componentType)
  checkArg(1, componentType, "string")
  if not getGroup().primaries[componentType] and not getGroup().adding[componentType] then
    -- This is mostly to avoid out of memory errors preventing proxy
    -- creation cause confusion by trying to create the proxy again,
    -- causing the oom error to be thrown again.
    component.setPrimary(componentType, component.list(componentType, true)())
  end
  return getGroup().primaries[componentType] ~= nil
end

function component.isPrimary(address)
  local componentType = component.type(address)
  if componentType then
    if component.isAvailable(componentType) then
      return getGroup().primaries[componentType].address == address
    end
  end
  return false
end

function component.getPrimary(componentType)
  checkArg(1, componentType, "string")
  assert(component.isAvailable(componentType),
    "no primary '" .. componentType .. "' available")
  return getGroup().primaries[componentType]
end

function component.setPrimary(componentType, address)
  checkArg(1, componentType, "string")
  checkArg(2, address, "string", "nil")
  if address ~= nil then
    address = component.get(address, componentType)
    assert(address, "no such component")
  end

  local wasAvailable = getGroup().primaries[componentType]
  if wasAvailable and address == wasAvailable.address then
    return
  end
  local wasAdding = getGroup().adding[componentType]
  if wasAdding and address == wasAdding.address then
    return
  end
  if wasAdding then
    kernel.modules.timer.remove(wasAdding.timer)
  end
  getGroup().primaries[componentType] = nil
  getGroup().adding[componentType] = nil

  local primary = address and component.proxy(address) or nil
  if wasAvailable then
    computer.pushSignal("component_unavailable", componentType)
  end
  if primary then
    if wasAvailable or wasAdding then
      getGroup().adding[componentType] = {
        address=address,
        timer=kernel.modules.timer.add(function()
          getGroup().adding[componentType] = nil
          getGroup().primaries[componentType] = primary
          --computer.pushSignal("component_available", componentType)
        end, 0.1)
      }
    else
      getGroup().primaries[componentType] = primary
      computer.pushSignal("component_available", componentType)
    end
  end
end

-------------------------------------------------------------------------------
function start()
    
    for address in component.list('screen') do
      if #component.invoke(address,'getKeyboards') > 0 then
        component.setPrimary('screen',address)
      end
    end

end

local function onComponentAdded(_, address, componentType)
  if not (getGroup().primaries[componentType] or getGroup().adding[componentType]) then
    component.setPrimary(componentType, address)
  end
end

local function onComponentRemoved(_, address, componentType)
  if getGroup().primaries[componentType] and getGroup().primaries[componentType].address == address or
     getGroup().adding[componentType] and getGroup().adding[componentType].address == address
  then
    component.setPrimary(componentType, component.list(componentType, true)())
  end
end

kernel.modules.keventd.listen("component_added", onComponentAdded)
kernel.modules.keventd.listen("component_removed", onComponentRemoved)
