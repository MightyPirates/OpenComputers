local resolutionX, resolutionY = nil, nil

-------------------------------------------------------------------------------

gpu = {}

function gpu.bind(screen)
  return driver.gpu.bind(component.primary("gpu"), screen)
end

function gpu.resolution(w, h)
  if w and h then
    return driver.gpu.resolution(component.primary("gpu"), w, h)
  elseif not resolutionX or not resolutionY then
    resolutionX, resolutionY = driver.gpu.resolution(component.primary("gpu"))
  end
  return resolutionX, resolutionY
end

function gpu.resolutions()
  return driver.gpu.resolutions(component.primary("gpu"))
end

function gpu.set(col, row, value)
  return driver.gpu.set(component.primary("gpu"), col, row, value)
end

function gpu.fill(col, row, w, h, value)
  return driver.gpu.fill(component.primary("gpu"), col, row, w, h, value)
end

function gpu.copy(col, row, w, h, tx, ty)
  return driver.gpu.copy(component.primary("gpu"), col, row, w, h, tx, ty)
end

-------------------------------------------------------------------------------

local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.isAvailable("gpu")) or
     (componentType == "gpu" and component.isAvailable("screen"))
  then
    gpu.bind(component.primary("screen"))
  end
end

local function onComponentUnavailable(_, componentType)
  if componentType == "gpu" or componentType == "screen" then
    resolutionX, resolutionY = nil, nil
  end
end

local function onScreenResized(_, address, width, height)
  if component.primary("screen") == address then
    resolutionX = width
    resolutionY = height
  end
end

return function()
  event.listen("component_available", onComponentAvailable)
  event.listen("component_unavailable", onComponentUnavailable)
  event.listen("screen_resized", onScreenResized)
end
