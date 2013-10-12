driver.network = {}

function driver.network.open(card, port)
  checkArg(1, card, "string")
  checkArg(2, port, "number")
  return send(card, "network.open", port)
end

function driver.network.close(card, port)
  checkArg(1, card, "string")
  checkArg(2, port, "number")
  return send(card, "network.close", port)
end

function driver.network.send(card, target, port, ...)
  checkArg(1, card, "string")
  checkArg(2, target, "string")
  checkArg(3, port, "number")
  return send(card, "network.send", target, port, ...)
end

function driver.network.broadcast(card, port, ...)
  checkArg(1, card, "string")
  checkArg(2, port, "number")
  return send(card, "network.broadcast", port, ...)
end
