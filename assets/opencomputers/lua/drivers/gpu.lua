--[[ API for graphics cards. ]]
driver.gpu = {}

function driver.gpu.setResolution(gpu, screen, w, h)
  sendToNode(gpu, "gpu.resolution=", screen, w, h)
end

function driver.gpu.getResolution(gpu, screen)
  return sendToNode(gpu, "gpu.resolution", screen)
end

function driver.gpu.getResolutions(gpu, screen)
  return sendToNode(gpu, "gpu.resolutions", screen)
end

function driver.gpu.set(gpu, screen, col, row, value)
  sendToNode(gpu, "gpu.set", screen, col, row, value)
end

function driver.gpu.fill(gpu, screen, col, row, w, h, value)
  sendToNode(gpu, "gpu.fill", screen, col, row, w, h, value:sub(1, 1))
end

function driver.gpu.copy(gpu, screen, col, row, w, h, tx, ty)
  sendToNode(gpu, "gpu.copy", screen, col, row, w, h, tx, ty)
end

function driver.gpu.bind(gpuId, screenId)
  return {
    setResolution = function(w, h)
     driver.gpu.setResolution(component.address(gpuId), component.address(screenId), w, h)
    end,
    getResolution = function()
     return driver.gpu.getResolution(component.address(gpuId), component.address(screenId))
    end,
    getResolutions = function()
     return driver.gpu.getResolutions(component.address(gpuId), component.address(screenId))
    end,
    set = function(col, row, value)
      driver.gpu.set(component.address(gpuId), component.address(screenId), col, row, value)
    end,
    fill = function(col, ro, w, h, value)
      driver.gpu.fill(component.address(gpuId), component.address(screenId), col, ro, w, h, value)
    end,
    copy = function(col, row, w, h, tx, ty)
      driver.gpu.copy(component.address(gpuId), component.address(screenId), col, row, w, h, tx, ty)
    end
  }
end