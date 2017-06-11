local component = require("component")
local computer = require("computer")
local event = require("event")

local adding = {}
local primaries = {}

-------------------------------------------------------------------------------

-- This allows writing component.modem.open(123) instead of writing
-- component.getPrimary("modem").open(123), which may be nicer to read.
setmetatable(component, {
  __index = function(_, key)
    return component.getPrimary(key)
  end,
  __pairs = function(self)
    local parent = false
    return function(_, key)
      if parent then
        return next(primaries, key)
      else
        local k, v = next(self, key)
        if not k then
          parent = true
          return next(primaries)
        else
          return k, v
        end
      end
    end
  end
})

function component.get(address, componentType)
  checkArg(1, address, "string")
  checkArg(2, componentType, "string", "nil")
  for c in component.list(componentType, true) do
    if c:sub(1, address:len()) == address then
      return c
    end
  end
  return nil, "no such component"
end

function component.isAvailable(componentType)
  checkArg(1, componentType, "string")
  if not primaries[componentType] and not adding[componentType] then
    -- This is mostly to avoid out of memory errors preventing proxy
    -- creation cause confusion by trying to create the proxy again,
    -- causing the oom error to be thrown again.
    component.setPrimary(componentType, component.list(componentType, true)())
  end
  return primaries[componentType] ~= nil
end

function component.isPrimary(address)
  local componentType = component.type(address)
  if componentType then
    if component.isAvailable(componentType) then
      return primaries[componentType].address == address
    end
  end
  return false
end

function component.getPrimary(componentType)
  checkArg(1, componentType, "string")
  assert(component.isAvailable(componentType),
    "no primary '" .. componentType .. "' available")
  return primaries[componentType]
end

function component.setPrimary(componentType, address)
  checkArg(1, componentType, "string")
  checkArg(2, address, "string", "nil")
  if address ~= nil then
    address = component.get(address, componentType)
    assert(address, "no such component")
  end

  local wasAvailable = primaries[componentType]
  if wasAvailable and address == wasAvailable.address then
    return
  end
  local wasAdding = adding[componentType]
  if wasAdding and address == wasAdding.address then
    return
  end
  if wasAdding then
    event.cancel(wasAdding.timer)
  end
  primaries[componentType] = nil
  adding[componentType] = nil

  local primary = address and component.proxy(address) or nil
  if wasAvailable then
    computer.pushSignal("component_unavailable", componentType)
  end
  if primary then
    if wasAvailable or wasAdding then
      adding[componentType] = {
        address=address,
        proxy = primary,
        timer=event.timer(0.1, function()
          adding[componentType] = nil
          primaries[componentType] = primary
          computer.pushSignal("component_available", componentType)
        end)
      }
    else
      primaries[componentType] = primary
      computer.pushSignal("component_available", componentType)
    end
  end
end

-------------------------------------------------------------------------------

local function onComponentAdded(_, address, componentType)
  local prev = primaries[componentType] or (adding[componentType] and adding[componentType].proxy)

  if prev then
    -- special handlers -- some components are just better at being primary
    if componentType == "screen" then
      --the primary has no keyboards but we do
      if #prev.getKeyboards() == 0 then
        local first_kb = component.invoke(address, 'getKeyboards')[1]
        if first_kb then
          -- just in case our kb failed to achieve primary
          -- possible if existing primary keyboard became primary first without a screen
          -- then prev (a screen) was added without a keyboard
          -- and then we attached this screen+kb pair, and our kb fired first - failing to achieve primary
          -- also, our kb may fire right after this, which is fine
          component.setPrimary("keyboard", first_kb)
          prev = nil -- nil meaning we should take this new one over the previous
        end
      end
    elseif componentType == "keyboard" then
      -- to reduce signal noise, if this kb is also the prev, we do not need to reset primary
      if address ~= prev.address then
        --keyboards never replace primary keyboards unless the are the only keyboard on the primary screen
        local current_screen = primaries.screen or (adding.screen and adding.screen.proxy)
        --if there is not yet a screen, do not use this keyboard, it's not any better
        if current_screen then
          -- the next phase is complicated
          -- there is already a screen and there is already a keyboard
          -- this keyboard is only better if this is a keyboard of the primary screen AND the current keyboard is not
          -- i don't think we can trust kb order (1st vs 2nd), 2nd could fire first
          -- but if there are two kbs on a screen, we can give preferred treatment to the first
          -- thus, assume 2nd is not attached for the purposes of primary kb
          -- and THUS, whichever (if either) is the 1st kb of the current screen
          -- this is only possible if
          -- 1. the only kb on the system (current) has no screen
          -- 2. a screen is added without a kb
          -- 3. this kb is added later manually

          -- prev is true when addr is not equal to the primary keyboard of the current screen -- meaning
          -- when addr is different, and thus it is not the primary keyboard, then we ignore this
          -- keyboard, and keep the previous
          -- prev is false means we should take this new keyboard
          prev = address ~= current_screen.getKeyboards()[1]
        end
      end
    end
  end

  if not prev then
    component.setPrimary(componentType, address)
  end
end

local function onComponentRemoved(_, address, componentType)
  if primaries[componentType] and primaries[componentType].address == address or
     adding[componentType] and adding[componentType].address == address
  then
    local next = component.list(componentType, true)()
    component.setPrimary(componentType, next)

    if componentType == "screen" and next then
      -- setPrimary already set the proxy (if successful)
      local proxy = (primaries.screen or (adding.screen and adding.screen.proxy))
      if proxy then
        -- if a screen is removed, and the primary keyboard is actually attached to another, non-primary, screen
        -- then the `next` screen, if it has a keyboard, should TAKE priority
        local next_kb = proxy.getKeyboards()[1] -- costly, don't call this method often
        local old_kb = primaries.keyboard or adding.keyboard
        -- if the next screen doesn't have a kb, this operation is without purpose, leave things as they are
        -- if there was no previous kb, use the new one
        if next_kb and (not old_kb or old_kb.address ~= next_kb) then
          component.setPrimary("keyboard", next_kb)
        end
      end
    end
  end
end

event.listen("component_added", onComponentAdded)
event.listen("component_removed", onComponentRemoved)

if _G.boot_screen then
  component.setPrimary("screen", _G.boot_screen)
end
_G.boot_screen = nil
