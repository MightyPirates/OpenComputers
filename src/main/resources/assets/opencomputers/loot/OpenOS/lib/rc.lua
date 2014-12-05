local fs = require('filesystem')

-- Keeps track of loaded scripts to retain local values between invocation
-- of their command callbacks.
local loaded = {}

local rc = {}

local function loadConfig()
  local env = {}
  local result, reason = loadfile('/etc/rc.cfg', 't', env)
  if result then
    result, reason = xpcall(result, debug.traceback)
    if result then
      return env
    end
  end
  return nil, reason
end

function rc.load(name, args)
  if loaded[name] then
    return loaded[name]
  end
  local fileName = fs.concat('/etc/rc.d/', name .. '.lua')
  local env = setmetatable({args = args}, {__index = _G})
  local result, reason = loadfile(fileName, 't', env)
  if result then
    result, reason = xpcall(result, debug.traceback)
    if result then
      loaded[name] = env
      return env
    end
  end
  return nil, reason
end

function rc.unload(name)
  loaded[name] = nil
end

local function rawRunCommand(name, cmd, args, ...)
  local result, what = rc.load(name, args)
  if result then
    if type(result[cmd]) == "function" then
      result, what = xpcall(result[cmd], debug.traceback, ...)
      if result then
        return true
      end
    else
      what = "Command '" .. cmd .. "' not found in daemon '" .. name .. "'"
    end
  end
  return nil, what
end

function rc.runCommand(name, cmd, ...)
  local conf, reason = loadConfig()
  if not conf then
    return nil, reason
  end
  return rawRunCommand(name, cmd, conf[name], ...)
end

function rc.allRunCommand(cmd, ...)
  local conf, reason = loadConfig()
  if not conf then
    return nil, reason
  end
  local results = {}
  for _, name in ipairs(conf.enabled or {}) do
    results[name] = table.pack(rawRunCommand(name, cmd, conf[name], ...))
  end
  return results
end

return rc
