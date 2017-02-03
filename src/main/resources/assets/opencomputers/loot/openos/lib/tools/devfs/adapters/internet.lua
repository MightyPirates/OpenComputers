return function(proxy)
  return
  {
    httpEnabled = {proxy.isHttpEnabled()},
    tcpEnabled = {proxy.isTcpEnabled()},
  }
end
