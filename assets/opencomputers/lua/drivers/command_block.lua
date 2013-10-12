driver.command = {}

function driver.command.value(block, value)
  checkArg(1, block, "string")
  if value then
    checkArg(2, value, "string")
    return send(block, "command.value=", value)
  else
    return send(block, "command.value")
  end
end

function driver.command.run(block)
  checkArg(1, block, "string")
  return send(block, "command.run")
end
