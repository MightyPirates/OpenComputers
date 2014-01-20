local event = require("event")

local function onComponentAvailable(_, componentType)
  local component = require("component")
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    component.gpu.bind(component.screen.address)
  end
end

event.listen("component_available", onComponentAvailable)
