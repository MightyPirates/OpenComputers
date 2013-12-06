local removing = {}
local primaries = {}

-------------------------------------------------------------------------------

-- This allows writing component.modem.open(123) instead of writing
-- component.getPrimary("modem").open(123), which may be nicer to read.
setmetatable(component, { __index = function(_, key)
                                      return component.getPrimary(key)
                                    end })

function component.get(address, componentType)
  checkArg(1, address, "string")
  checkArg(2, componentType, "string", "nil")
  for c in component.list(componentType) do
    if c:sub(1, address:len()) == address then
      return c
    end
  end
  return nil, "no such component"
end

function component.isAvailable(componentType)
  checkArg(1, componentType, "string")
  return primaries[componentType] ~= nil
end

function component.isPrimary(address)
  local componentType = component.type(address)
  if componentType then
    if component.isAvailable(componentType) then
      return primaries[componentType].address == address
    end
  end
  return false
end

function component.getPrimary(componentType)
  checkArg(1, componentType, "string")
  assert(component.isAvailable(componentType),
    "no primary '" .. componentType .. "' available")
  return primaries[componentType]
end

function component.setPrimary(componentType, address)
  checkArg(1, componentType, "string")
  checkArg(2, address, "string", "nil")
  if address ~= nil then
    address = component.get(address, componentType)
    assert(address, "no such component")
  end
  local wasAvailable = component.isAvailable(componentType)
  if wasAvailable and address == primaries[componentType].address then
    return
  end
  primaries[componentType] = address and component.proxy(address) or nil
  if wasAvailable then
    computer.pushSignal("component_unavailable", componentType)
  end
  if component.isAvailable(componentType) then
    computer.pushSignal("component_available", componentType)
  end
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address, componentType)
  if not component.isAvailable(componentType) then
    component.setPrimary(componentType, address)
  end
end

local function onComponentRemoved(_, address, componentType)
  if component.isAvailable(componentType) and
    component.getPrimary(componentType).address == address
  then
    component.setPrimary(componentType, component.list(componentType)())
  end
end

return function()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end
