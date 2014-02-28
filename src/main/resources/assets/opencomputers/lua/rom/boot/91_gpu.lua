local component = require("component")
local event = require("event")

local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    component.gpu.bind(component.screen.address)
    local depth = 2^(component.gpu.getDepth())
    os.setenv("TERM", "term-"..depth.."color")
  end
end

event.listen("component_available", onComponentAvailable)
