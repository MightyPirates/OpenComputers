network = {}
net = network

function network.open(port)
  return driver.network.open(component.primary("network"), port)
end

function network.close(port)
  return driver.network.close(component.primary("network"), port)
end

function network.send(target, port, ...)
  return driver.network.send(component.primary("network"), target, port, ...)
end

function network.broadcast(port, ...)
  return driver.network.broadcast(component.primary("network"), port, ...)
end
