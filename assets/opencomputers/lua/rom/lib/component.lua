local components = {}
local removing = {}
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
    local address
    if args[1] ~= nil then
      for c in component.list(componentType) do
        if c:usub(1, args[1]:ulen()) == args[1] then
          address = c
          break
        end
      end
      assert(address, "no such component")
    end
    local wasAvailable = component.isAvailable(componentType)
    primaries[componentType] = address
    if not wasAvailable and component.isAvailable(componentType) then
      event.fire("component_available", componentType)
    elseif wasAvailable and not component.isAvailable(componentType) then
      event.fire("component_unavailable", componentType)
    end
  else
    assert(component.isAvailable(componentType),
      "no primary '" .. componentType .. "' available")
    return primaries[componentType]
  end
end

function component.type(address)
  return components[address]
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  if components[address] then
    return false -- cancel this event, it is invalid
  end
  local componentType = driver.componentType(address)
  if not componentType then
    return -- component was removed again before signal could be processed
  end
  components[address] = componentType
  if not component.isAvailable(componentType) then
    component.primary(componentType, address)
  end
end

local function onComponentRemoved(_, address)
  if removing[address] then return end
  if not components[address] then return false end
  local componentType = component.type(address)
  components[address] = nil
  -- Redispatch with component type, since we already removed.
  removing[address] = true -- don't cancel this one!
  event.fire("component_removed", address, componentType)
  removing[address] = false
  if primaries[componentType] == address then
    component.primary(componentType, nil)
    address = component.list(componentType)()
    component.primary(componentType, address)
  end
  return false -- cancel this one
end

return function()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end
