local adapter_api = ...

return function(proxy)
  local screen = proxy.getScreen()
  screen = screen and ("../" .. screen)
  return
  {
    viewport = {write = adapter_api.createWriter(proxy.setViewport, 2, "number", "number"), proxy.getViewport()},
    resolution = {write = adapter_api.createWriter(proxy.setResolution, 2, "number", "number"), proxy.getResolution()},
    maxResolution = {proxy.maxResolution()},
    screen = {link=screen,isAvailable=proxy.getScreen},
    depth = {write = adapter_api.createWriter(proxy.setDepth, 1, "number"), proxy.getDepth()},
    maxDepth = {proxy.maxDepth()},
    background = {write = adapter_api.createWriter(proxy.setBackground, 1, "number", "boolean"), proxy.getBackground()},
    foreground = {write = adapter_api.createWriter(proxy.setForeground, 1, "number", "boolean"), proxy.getForeground()},
  }
end
