local event = require("event")

local function onComponentAvailable(_, componentType)
  local component = require("component")
  local tty = require("tty")
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
    event.push("gpu_bound", gpu.address, screen_address)
    if tty.gpu() ~= gpu then
      tty.bind(gpu)
      event.push("term_available")
    end
  end
end

event.listen("component_available", onComponentAvailable)
