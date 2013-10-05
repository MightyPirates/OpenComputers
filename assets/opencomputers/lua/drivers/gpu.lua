driver.gpu = {}

function driver.gpu.setResolution(gpu, screen, w, h)
  send(gpu, "gpu.resolution=", screen, w, h)
end

function driver.gpu.getResolution(gpu, screen)
  return send(gpu, "gpu.resolution", screen)
end

function driver.gpu.getResolutions(gpu, screen)
  return send(gpu, "gpu.resolutions", screen)
end

function driver.gpu.set(gpu, screen, col, row, value)
  send(gpu, "gpu.set", screen, col, row, value)
end

function driver.gpu.fill(gpu, screen, col, row, w, h, value)
  send(gpu, "gpu.fill", screen, col, row, w, h, value:sub(1, 1))
end

function driver.gpu.copy(gpu, screen, col, row, w, h, tx, ty)
  send(gpu, "gpu.copy", screen, col, row, w, h, tx, ty)
end

function driver.gpu.bind(gpu, screen)
  return {
    setResolution = function(w, h)
     driver.gpu.setResolution(gpu, screen, w, h)
    end,
    getResolution = function()
     return driver.gpu.getResolution(gpu, screen)
    end,
    getResolutions = function()
     return driver.gpu.getResolutions(gpu, screen)
    end,
    set = function(col, row, value)
      driver.gpu.set(gpu, screen, col, row, value)
    end,
    fill = function(col, ro, w, h, value)
      driver.gpu.fill(gpu, screen, col, ro, w, h, value)
    end,
    copy = function(col, row, w, h, tx, ty)
      driver.gpu.copy(gpu, screen, col, row, w, h, tx, ty)
    end
  }
end