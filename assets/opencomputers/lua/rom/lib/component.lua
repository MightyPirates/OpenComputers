local components = {}
local primaries = {}

-------------------------------------------------------------------------------

component = {}

function component.isAvailable(componentType)
  return primaries[componentType] ~= nil
end

function component.list(filter)
  local address, ctype = nil
  return function()
    repeat
      address, ctype = next(components, address)
    until not address or type(filter) ~= "string" or ctype:match(filter)
    return address, ctype
  end
end

function component.primary(componentType, ...)
  checkArg(1, componentType, "string")
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(2, args[1], "string", "nil")
    local wasAvailable = component.isAvailable(componentType)
    primaries[componentType] = args[1]
    if not wasAvailable and component.isAvailable(componentType) then
      event.fire("component_available", componentType)
    elseif wasAvailable and not component.isAvailable(componentType) then
      event.fire("component_unavailable", componentType)
    end
  else
    assert(component.isAvailable(componentType),
      "no primary " .. componentType .. " available")
    return primaries[componentType]
  end
end

function component.type(address)
  return components[address]
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  local componentType = driver.componentType(address)
  components[address] = componentType
  if not component.isAvailable(componentType) then
    component.primary(componentType, address)
  end
end

local function onComponentRemoved(_, address)
  local componentType = component.type(address)
  components[address] = nil
  if primaries[componentType] == address then
    component.primary(componentType, nil)
    for address in component.list() do
      if component.type(address) == componentType then
        component.primary(componentType, address)
        return
      end
    end
  end
end

function component.install()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end

function component.uninstall()
  event.ignore("component_added", onComponentAdded)
  event.ignore("component_removed", onComponentRemoved)
end
