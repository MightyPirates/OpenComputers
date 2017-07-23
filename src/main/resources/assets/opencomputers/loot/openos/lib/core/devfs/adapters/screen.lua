local adapter_api = ...

return function(proxy)
  return
  {
    ["aspectRatio"] = {proxy.getAspectRatio()},
    ["keyboards"] = {read=function()
      local ks = {}
      for _,ka in ipairs(proxy.getKeyboards()) do
        table.insert(ks, ka)
      end
      return table.concat(ks, "\n")
    end},
    ["on"] = adapter_api.create_toggle(proxy.isOn, proxy.turnOn, proxy.turnOff), -- turnOn and turnOff
    ["precise"] = adapter_api.create_toggle(proxy.isPrecise, proxy.setPrecise),
    ["touchModeInverted"] = adapter_api.create_toggle(proxy.isTouchModeInverted, proxy.setTouchModeInverted),
  }
end
