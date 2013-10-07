local components = {}

-------------------------------------------------------------------------------

component = {}

function component.type(address)
  return components[address]
end

function component.list()
  local address = nil
  return function()
    address = next(components, address)
    return address
  end
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address)
  components[address] = driver.componentType(address)
end

local function onComponentRemoved(_, address)
  components[address] = nil
end

function component.install()
  event.listen("component_added", onComponentAdded)
  event.listen("component_removed", onComponentRemoved)
end

function component.uninstall()
  event.ignore("component_added", onComponentAdded)
  event.ignore("component_removed", onComponentRemoved)
end
