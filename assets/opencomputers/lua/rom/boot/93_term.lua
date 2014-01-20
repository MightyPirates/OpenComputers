local computer = require("computer")
local event = require("event")

local gpuAvailable, screenAvailable = false, false

local function isAvailable()
  return gpuAvailable and screenAvailable
end

local function onComponentAvailable(_, componentType)
  local wasAvailable = isAvailable()
  if componentType == "gpu" then
    gpuAvailable = true
  elseif componentType == "screen" then
    screenAvailable = true
  end
  if not wasAvailable and isAvailable() then
    computer.pushSignal("term_available")
  end
end

local function onComponentUnavailable(_, componentType)
  local wasAvailable = isAvailable()
  if componentType == "gpu" then
    gpuAvailable = false
  elseif componentType == "screen" then
    screenAvailable = false
  end
  if wasAvailable and not isAvailable() then
    computer.pushSignal("term_unavailable")
  end
end

event.listen("component_available", onComponentAvailable)
event.listen("component_unavailable", onComponentUnavailable)
