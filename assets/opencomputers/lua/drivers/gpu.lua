driver.gpu = {}

function driver.gpu.bind(gpu, screen)
  checkArg(1, gpu, "string")
  checkArg(2, screen, "string")
  return send(gpu, "gpu.bind", screen)
end

function driver.gpu.resolution(gpu, w, h)
  checkArg(1, gpu, "string")
  if w and h then
    checkArg(2, w, "number")
    checkArg(3, h, "number")
    return send(gpu, "gpu.resolution=", w, h)
  else
    return send(gpu, "gpu.resolution")
  end
end

function driver.gpu.maxResolution(gpu)
  checkArg(1, gpu, "string")
  return send(gpu, "gpu.maxResolution")
end

function driver.gpu.set(gpu, col, row, value)
  checkArg(1, gpu, "string")
  checkArg(2, col, "number")
  checkArg(3, row, "number")
  checkArg(4, value, "string")
  return send(gpu, "gpu.set", col, row, value)
end

function driver.gpu.fill(gpu, col, row, w, h, value)
  checkArg(1, gpu, "string")
  checkArg(2, col, "number")
  checkArg(3, row, "number")
  checkArg(4, w, "number")
  checkArg(5, h, "number")
  checkArg(6, value, "string")
  return send(gpu, "gpu.fill", col, row, w, h, value:usub(1, 1))
end

function driver.gpu.copy(gpu, col, row, w, h, tx, ty)
  checkArg(1, gpu, "string")
  checkArg(2, col, "number")
  checkArg(3, row, "number")
  checkArg(4, w, "number")
  checkArg(5, h, "number")
  checkArg(6, tx, "number")
  checkArg(7, ty, "number")
  return send(gpu, "gpu.copy", col, row, w, h, tx, ty)
end
