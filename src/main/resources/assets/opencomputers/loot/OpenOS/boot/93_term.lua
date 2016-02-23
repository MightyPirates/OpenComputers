local component = require("component")
local computer = require("computer")
local event = require("event")
local keyboard = require("keyboard")
local term = require("term")

local gpuAvailable, screenAvailable = false, false

local function isAvailable()
  return gpuAvailable and screenAvailable
end

local function onComponentAvailable(_, componentType)
  local wasAvailable = isAvailable()
  if componentType == "gpu" then
    gpuAvailable = true
  elseif componentType == "screen" then
    screenAvailable = true
  end
  if not wasAvailable and isAvailable() then
    computer.pushSignal("term_available")
  end
end

local function onComponentUnavailable(_, componentType)
  local wasAvailable = isAvailable()
  if componentType == "gpu" then
    gpuAvailable = false
  elseif componentType == "screen" then
    screenAvailable = false
  end
  if wasAvailable and not isAvailable() then
    computer.pushSignal("term_unavailable")
  end
end

local function onTermFocus(event, screenAddress)
  term.__focus[screenAddress] = term.__nextFocus[screenAddress]
end

local function updateKeyboards(event, _, typ)
  if event == nil or typ == "screen" or typ == "keyboard" then
    term.__keyboardToScreen = {}
    term.__screenToKeyboard = {}
    for screenAddress in component.list("screen", true) do
      local keyboardAddress = component.invoke(screenAddress, "getKeyboards")[1]
      if keyboardAddress then
        term.__keyboardToScreen[keyboardAddress] = screenAddress
        term.__screenToKeyboard[screenAddress] = keyboardAddress
      end
    end
  end
end

local function onKeyDown(event, keyboardAddress, char, code)
  local screenAddress = term.__keyboardToScreen[keyboardAddress]
  if screenAddress then
    local self = term.__focus[screenAddress]
    if self then
      if code == keyboard.keys.tab then
        if keyboard.isControlDown(keyboardAddress) then
          if keyboard.isShiftDown(keyboardAddress) then
            self:focusPrevious()
          else
            self:focusNext()
          end
        end
      end
    end
  end
end

local function onTouch(event, screenAddress, x, y)
  local list = term.__knownWindows[screenAddress]
  if list[1] then
    local _, gpu = list[1]:getGPU()
    if gpu and gpu.getScreen() == screenAddress then
      local w, h = gpu.getViewport()
      if w then
        local nextWindow
        for _, window in ipairs(list) do
          local xWin, yWin, wWin, hWin = window:getGlobalArea(w, h)
          local xOffset, yOffset = x - xWin, y - yWin
          if xOffset >= 0 and xOffset < wWin and yOffset >= 0 and yOffset < hWin then
            nextWindow = window
          end
        end
        if nextWindow then
          nextWindow:focus()
        end
      end
    end
  end
end

event.listen("component_available", onComponentAvailable)
event.listen("component_unavailable", onComponentUnavailable)
event.listen("component_added", updateKeyboards)
event.listen("component_removed", updateKeyboards)
event.listen("term_focus", onTermFocus)
event.listen("key_down", onKeyDown)
event.listen("touch", onTouch)


