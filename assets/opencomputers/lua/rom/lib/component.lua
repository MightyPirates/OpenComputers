local removing = {}
local primaries = {}

local function tryGetPrimary(_, key)
  if component.isAvailable(key) then
    return primaries[key]
  end
end

-------------------------------------------------------------------------------

-- This allows writing component.modem.open(123) instead of writing
-- component.primary("modem").open(123), which may be nicer to read.
setmetatable(component, {__index=tryGetPrimary})

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

function component.primary(componentType, ...)
  checkArg(1, componentType, "string")
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(2, args[1], "string", "nil")
    local address
    if args[1] ~= nil then
      address = component.get(args[1], componentType)
      assert(address, "no such component")
    end
    local wasAvailable = component.isAvailable(componentType)
    primaries[componentType] = address and component.proxy(address) or nil
    if component.isAvailable(componentType) then
      os.pushSignal("component_available", componentType)
    elseif wasAvailable then
      os.pushSignal("component_unavailable", componentType)
    end
  else
    assert(component.isAvailable(componentType),
      "no primary '" .. componentType .. "' available")
    return primaries[componentType]
  end
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address, componentType)
  if not component.isAvailable(componentType) then
    component.primary(componentType, address)
  end
end

local function onComponentRemoved(_, address, componentType)
  if component.isAvailable(componentType) and
    component.primary(componentType).address == address
  then
    component.primary(componentType, nil)
    address = component.list(componentType)()
    component.primary(componentType, address)
  end
end

return function()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end
