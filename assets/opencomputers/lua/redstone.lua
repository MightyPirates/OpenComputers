--[[ API for Redstone Cards. ]]

driver.redstone = {}

driver.redstone.sides = {"top", "bottom", "left", "right", "front", "back"}

-- Add inverse mapping and aliases.
for k, v in pairs(sides) do sides[v] = k end
sides.up = sides.top
sides.down = sides.bottom

function driver.redstone.getAnalogInput(card, side)
  checkArg(1, side, "number")
  sendToNode(card, os.address(), "redstone.input", side)
end

function driver.redstone.getAnalogOutput(card, side)
  checkArg(1, side, "number")
  sendToNode(card, os.address(), "redstone.output", side)
end

function driver.redstone.setAnalogOutput(card, side, value)
  checkArg(1, side, "number")
  checkArg(2, side, "number")
  sendToNode(card, os.address(), "redstone.output=", side, value)
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
