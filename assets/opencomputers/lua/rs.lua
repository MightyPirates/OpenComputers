--[[
  The Redstone API, provided by Redstone components.
]]

sides = {"top", "bottom", "left", "right", "front", "back"}

function getSides()
  return sides
end

function getInput(side)
  return getAnalogInput(side) > 0
end

function getOutput(side)
  return getAnalogOutput(side) > 0
end

function setOutput(side, value)
  rs.setAnalogOutput(side, value and 15 or 0)
end

getAnalogInput = _G.getAnalogInput
getAnalogOutput = _G.getAnalogOutput
setAnalogOutput = _G.setAnalogOutput