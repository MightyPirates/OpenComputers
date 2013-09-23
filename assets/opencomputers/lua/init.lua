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
          driver.gpu.fill(idGpu, idScreen, 1, resY, resX, 1, " ")
          cursorY = resY
        end
      end
      checkCursor()
      for line, nl in value:gmatch("([^\r\n]*)([\r\n]?)") do
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
        if nl:len() == 1 then
          cursorX = 1
          cursorY = cursorY + 1
          checkCursor()
        end
      end
    end
  }
end

local components = {}
function onInstall(id)
  components[id] = driver.componentType(id)
  print("Component installed: " .. id .. " (" .. components[id] .. ")")

  local function hello(idGpu, idScreen)
    term = newTerm(idGpu, idScreen)
    write = function(...)
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
  local signal, param = os.signal(nil, 2)
  if signal == "component_added" then
    onInstall(param)
  elseif signal == "component_removed" then
    onUninstall(param)
  elseif signal == "key_down" then
    write(param)
  else
    write("Clock: ")
    print(os.clock())
  end
end