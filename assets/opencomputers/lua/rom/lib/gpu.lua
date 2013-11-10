local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    local gpu = component.primary("gpu")
    gpu.bind(component.primary("screen").address)
  end
end

return function()
  event.listen("component_available", onComponentAvailable)
end
