local adapter_api = ...

return function(proxy)
  return
  {
    beep = {write=adapter_api.createWriter(proxy.beep, 0, "number", "number")},
    running = adapter_api.create_toggle(proxy.isRunning, proxy.start, proxy.stop),
  }
end
