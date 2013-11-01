local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    local gpu = component.primary("gpu")
    gpu.bind(component.primary("screen").address)
    local maxX, maxY = gpu.maxResolution()
    gpu.setResolution(maxX, maxY)
  end
end

return function()
  event.listen("component_available", onComponentAvailable)
end
