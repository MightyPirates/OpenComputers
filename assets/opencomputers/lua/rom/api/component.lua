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

event.listen("component_added", function(_, address)
  components[address] = driver.componentType(address)
end)

event.listen("component_removed", function(_, address)
  components[address] = nil
end)