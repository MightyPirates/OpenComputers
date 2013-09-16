--[[ Top level program run by the kernel. ]]

function newTerm(idGpu, idScreen)
  local cursorX, cursorY = 1, 1
  return {
    getCursor = function()
      return cursorX, cursorY
    end,
    setCursor = function(col, row)
      if type(col) == "number" then cursorX = col end
      if type(row) == "number" then cursorY = row end
    end,
    write = function(value, wrap)
      value = tostring(value)
      if idGpu <= 0 or value:len() == 0 then return end
      local resX, resY = driver.gpu.getResolution(idGpu, idScreen)
      local function checkCursor()
        if cursorX > resX then
          cursorX = 1
          cursorY = cursorY + 1
        end
        if cursorY > resY then
          driver.gpu.copy(idGpu, idScreen, 1, 1, resX, resY, 0, -1)
          driver.gpu.fill(idGpu, idScreen, 1, resY, 1, resY, " ")
          cursorY = resY
        end
      end
      checkCursor()
      local first = true
      for line in value:gmatch("[^\r\n]*") do
        if not first then
          cursorX = 1
          cursorY = cursorY + 1
          checkCursor()
        end
        first = false
        while wrap and line:len() > resX - cursorX + 1 do
          local partial = line:sub(1, resX - cursorX + 1)
          line = line:sub(partial:len() + 1)
          driver.gpu.set(idGpu, idScreen, cursorX, cursorY, partial)
          cursorX = cursorX + partial:len()
          checkCursor()
        end
        if line:len() > 0 then
          driver.gpu.set(idGpu, idScreen, cursorX, cursorY, line)
          cursorX = cursorX + line:len()
          checkCursor()
        end
      end
    end
  }
end

local components = {}
function onInstall(id)
  components[id] = component.type(id)
  print("Component installed: " .. id .. " (" .. components[id] .. ")")

  local function hello(idGpu, idScreen)
    term = newTerm(idGpu, idScreen)
    print = function(...)
      local args = {...}
      for _, value in ipairs(args) do
        term.write(value, true)
      end
    end
    print("gpu: " .. idGpu .. " screen: " .. idScreen)
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
  local signal, id = os.signal(nil, 3)
  if signal == "component_install" then
    onInstall(id)
  elseif signal == "component_uninstall" then
    onUninstall(id)
  end
  print(os.clock())
end