local event = require("event")
local computer = require("computer")
local su = require("superUtiles")

local function onComponentAvailable(_, componentType)
  local component = require("component")
  local tty = require("tty")
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    local gpu, screen = component.gpu, component.screen
    local screen_address = screen.address
    if gpu.getScreen() ~= screen_address then
      if not pcall(gpu.bind, screen_address, false) then --для слишком старых версий самого open computers
        gpu.bind(screen_address)
      end
      if computer.setBootScreen then pcall(computer.setBootScreen, screen_address) end
      pcall(su.saveFile, tostring(screen_address))
    end
    local depth = math.floor(2^(gpu.getDepth()))
    os.setenv("TERM", "term-"..depth.."color")
    event.push("gpu_bound", gpu.address, screen_address)
    if tty.gpu() ~= gpu then
      tty.bind(gpu)
      event.push("term_available")
    end
    os.execute("rescreen")
    if refreshKeyboards then
      refreshKeyboards()
    end
  end
end

event.listen("component_available", onComponentAvailable)
