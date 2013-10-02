driver.redstone = {}
driver.rs = driver.redstone

driver.redstone.sides = {"top", "bottom", "left", "right", "front", "back"}

-- Add inverse mapping and aliases.
for k, v in pairs(driver.redstone.sides) do
  driver.redstone.sides[v] = k
end
driver.redstone.sides.up = driver.redstone.sides.top
driver.redstone.sides.down = driver.redstone.sides.bottom

-- Save this before there's a chance it gets changed by a user.
local owner = os.address()

function driver.redstone.analogInput(card, side)
  sendToNode(card, owner, "redstone.input", side)
end

function driver.redstone.analogOutput(card, side, value)
  if value then
    sendToNode(card, owner, "redstone.output=", side, tonumber(value))
  else
    return sendToNode(card, owner, "redstone.output", side)
  end
end

function driver.redstone.input(card, side)
  return driver.redstone.analogInput(card, side) > 0
end

function driver.redstone.output(card, side, value)
  if value then
    driver.redstone.analogOutput(side, value and 15 or 0)
  else
    return driver.redstone.analogOutput(card, side) > 0
  end
end
