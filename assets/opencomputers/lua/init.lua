--[[ Top level program run by the kernel. ]]

local components = {}
function onInstall(id)
  components[id] = component.type(id)
  print("Component installed: " .. id .. " (" .. components[id] .. ")")

  local function hello(idGpu, idScreen)
    print("gpu: " .. idGpu .. " screen: " .. idScreen)
    driver.gpu.bind(idGpu, idScreen)
    driver.gpu.set(idGpu, 1, 1, "Hello world!")
  end
  if components[id] == "gpu" then
    for otherId, otherType in pairs(components) do
      if otherType == "screen" then
        hello(id, otherId)
      end
    end
  elseif components[id] == "screen" then
    for otherId, otherType in pairs(components) do
      if otherType == "gpu" then
        hello(otherId, id)
      end
    end
  end
end

function onUninstall(id)
  components[id] = nil
  print("Component uninstalled: " .. id)
end

-- Main OS loop, keeps everything else running.
while true do
  local signal, id = os.signal()
  if signal == "component_install" then
    onInstall(id)
  elseif signal == "component_uninstall" then
    onUninstall(id)
  end
end