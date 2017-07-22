local fs = require('filesystem')
local serialization = require('serialization')

-- Keeps track of loaded scripts to retain local values between invocation
-- of their command callbacks.
local loaded = {}

local rc = {}

local function saveConfig(conf)
  local file, reason = io.open('/etc/rc.cfg', 'w')
  if not file then
    return nil, reason
  end
  for key, value in pairs(conf) do
    file:write(tostring(key) .. " = " .. serialization.serialize(value) .. "\n")
  end
  
  file:close()
  return true
end

local function loadConfig()
  local env = {}
  if not fs.exists('/etc/rc.cfg') then
    saveConfig({enabled={}})
  end
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

local function rawRunCommand(conf, name, cmd, args, ...)
  local result, what = rc.load(name, args)
  if result then
    if not cmd then
      io.output():write("Commands for service " .. name .. "\n")
      for command, val in pairs(result) do
        if type(val) == "function" then
          io.output():write(tostring(command) .. " ")
        end
      end
      return true
    elseif type(result[cmd]) == "function" then
      res, what = xpcall(result[cmd], debug.traceback, ...)
      if res then
        return true
      end
    elseif cmd == "restart" and type(result["stop"]) == "function" and type(result["start"]) == "function" then
      res, what = xpcall(result["stop"], debug.traceback, ...)
      if res then
        res, what = xpcall(result["start"], debug.traceback, ...)
        if res then
          return true
        end
      end
    elseif cmd == "enable" then
      conf.enabled = conf.enabled or {}
      for _, _name in ipairs(conf.enabled) do
        if name == _name then
          return nil, "Service already enabled"
        end
      end
      conf.enabled[#conf.enabled + 1] = name
      return saveConfig(conf)
    elseif cmd == "disable" then
      conf.enabled = conf.enabled or {}
      for n, _name in ipairs(conf.enabled) do
        if name == _name then
          table.remove(conf.enabled, n)
        end
      end
      return saveConfig(conf)
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
  return rawRunCommand(conf, name, cmd, conf[name], ...)
end

function rc.allRunCommand(cmd, ...)
  local conf, reason = loadConfig()
  if not conf then
    return nil, reason
  end
  local results = {}
  for _, name in ipairs(conf.enabled or {}) do
    results[name] = table.pack(rawRunCommand(conf, name, cmd, conf[name], ...))
  end
  return results
end

return rc
