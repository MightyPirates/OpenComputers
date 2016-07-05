local component = require("component")
local computer = require("computer")
local event = require("event")
local term = require("term")
local process = require("process")

-- this should be the init level process
process.info().data.window = term.internal.open()

event.listen("gpu_bound", function(ename, gpu, screen)
  gpu=component.proxy(gpu)
  screen=component.proxy(screen)
  term.bind(gpu, screen)
  computer.pushSignal("term_available")
end)

event.listen("component_unavailable", function(_,type)
  if type == "screen" or type == "gpu" or type == "keyboard" then
    if term.isAvailable() then
      local window = term.internal.window()
      if window[type] and not component.proxy(window[type].address) then
        window[type] = nil
      end
    end
    if not term.isAvailable() then
      computer.pushSignal("term_unavailable")
    end
  end
end)

event.listen("component_available", function(_,type)
  -- we need to clear the current terminals cached keyboard (if any) when a new keyboard becomes available
  -- this is in case the new keyboard was attached to the terminal's window
  -- the terminal library has the code to determine what the best keyboard to use is
  -- but here, we'll just set the cache to nil to force term library to reload it
  -- an alternative to this method would be to make sure the terminal library doesn't cache the wrong keybaord to begin with
  -- but, users may actually expect that any primary keyboard is a valid keyboard (weird, in my opinion)
  if type == "keyboard" then
    local window = term.internal.window()
    if window then
      window.keyboard = nil
    end
  end
end)

event.listen("screen_resized", function(_,addr,w,h)
  local window = term.internal.window()
  if term.isAvailable(window) and window.screen.address == addr and window.fullscreen then
    window.w,window.h = w,h
  end
end)
