local rc = require("rc")
local fs = require("filesystem")

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

local function saveConfig(conf)
  local file, reason = io.open('/etc/rc.cfg', 'w')
  if not file then
    return nil, reason
  end
  for key, value in pairs(conf) do
    file:write(tostring(key) .. " = " .. require("serialization").serialize(value) .. "\n")
  end
  
  file:close()
  return true
end

local function load(name, args)
  if rc.loaded[name] then
    return rc.loaded[name]
  end
  local fileName = fs.concat('/etc/rc.d/', name .. '.lua')
  local env = setmetatable({args = args}, {__index = _G})
  local result, reason = loadfile(fileName, 't', env)
  if result then
    result, reason = xpcall(result, debug.traceback)
    if result then
      rc.loaded[name] = env
      return env
    else
      return nil, string.format("%s failed to start: %s", fileName, reason)
    end
  end
  return nil, string.format("%s failed to load: %s", fileName, reason)
end

function rc.unload(name)
  rc.loaded[name] = nil
end

local function rawRunCommand(conf, name, cmd, args, ...)
  local result, what = load(name, args)
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
      result, what = xpcall(result[cmd], debug.traceback, ...)
      if result then
        return true
      end
    elseif cmd == "restart" and type(result["stop"]) == "function" and type(result["start"]) == "function" then
      local daemon = result
      result, what = xpcall(daemon["stop"], debug.traceback, ...)
      if result then
        result, what = xpcall(daemon["start"], debug.traceback, ...)
        if result then
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

local function runCommand(name, cmd, ...)
  local conf, reason = loadConfig()
  if not conf then
    return nil, reason
  end
  return rawRunCommand(conf, name, cmd, conf[name], ...)
end

local function allRunCommand(cmd, ...)
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

local stream = io.stderr
local write = stream.write

if select("#", ...) == 0 then
  -- if called during boot, pipe errors to onError handler
  if _G.runlevel == "S" then
    write = function(_, msg)
      require("event").onError(msg)
    end
  end

  local results, reason = allRunCommand("start")
  if not results then
    local msg = "rc failed to start:"..tostring(reason)
    write(stream, msg, "\n")
    return
  end
  for _, result in pairs(results) do
    local ok, reason = table.unpack(result)
    if not ok then
      write(stream, reason, "\n")
    end
  end
else
  local result, reason = runCommand(...)
  if not result then
    write(stream, reason, "\n")
    return 1
  end
end
