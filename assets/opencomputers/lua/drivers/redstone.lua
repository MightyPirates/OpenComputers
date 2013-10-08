driver.redstone = {}

-- Save this before there's a chance it gets changed by a user.
local owner = os.address()

function driver.redstone.analogInput(card, side)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  return send(card, "redstone.input", owner, side)
end

function driver.redstone.analogOutput(card, side, value)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  checkArg(3, value, "number", "nil")
  if value then
    return send(card, "redstone.output=", owner, side, value)
  else
    return send(card, "redstone.output", owner, side)
  end
end

function driver.redstone.input(card, side)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  return driver.redstone.analogInput(card, side) > 0
end

function driver.redstone.output(card, side, value)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  checkArg(3, value, "boolean", "nil")
  if value ~= nil then
    return driver.redstone.analogOutput(card, side, value and 15 or 0)
  else
    return driver.redstone.analogOutput(card, side) > 0
  end
end
