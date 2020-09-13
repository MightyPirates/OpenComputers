return function(proxy)
  return
  {
    wakeMessage =
    {
      read = function() return proxy.getWakeMessage() or "" end,
      write= function(msg) return proxy.setWakeMessage(msg) end,
    },
    wireless = {proxy.isWireless()},
  }
end
