driver.redstone = {}
driver.rs = driver.redstone

driver.redstone.sides = {"top", "bottom", "left", "right", "front", "back"}

-- Add inverse mapping and aliases.
for k, v in pairs(driver.redstone.sides) do
  driver.redstone.sides[v] = k
end
driver.redstone.sides.up = driver.redstone.sides.top
driver.redstone.sides.down = driver.redstone.sides.bottom

local safeOsAddress = os.address

function driver.redstone.analogInput(card, side)
  sendToNode(card, safeOsAddress(), "redstone.input", side)
end

function driver.redstone.analogOutput(card, side, value)
  if value then
    sendToNode(card, safeOsAddress(), "redstone.output=", side, value)
  else
    return sendToNode(card, safeOsAddress(), "redstone.output", side)
  end
end

function driver.redstone.input(card, side)
  return driver.redstone.analogInput(card, side) > 0
end

function output(card, side, value)
  if value then
    driver.redstone.analogOutput(side, value and 15 or 0)
  else
    return driver.redstone.analogOutput(card, side) > 0
  end
end
