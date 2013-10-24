driver.redstone = {}

function driver.redstone.analogInput(card, side)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  return send(card, "redstone.input", side)
end

function driver.redstone.analogOutput(card, side, value)
  checkArg(1, card, "string")
  checkArg(2, side, "number")
  checkArg(3, value, "number", "nil")
  if value then
    return send(card, "redstone.output=", side, value)
  else
    return send(card, "redstone.output", side)
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
