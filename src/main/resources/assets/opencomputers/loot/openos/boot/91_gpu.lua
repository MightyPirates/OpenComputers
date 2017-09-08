local component = require("component")
local event = require("event")

local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    local gpu, screen = component.gpu, component.screen
    local screen_address = screen.address
    if gpu.getScreen() ~= screen_address then
      gpu.bind(screen_address)
    end
    local depth = math.floor(2^(gpu.getDepth()))
    os.setenv("TERM", "term-"..depth.."color")
    require("computer").pushSignal("gpu_bound", gpu.address, screen_address)
  end
end

event.listen("component_available", onComponentAvailable)
