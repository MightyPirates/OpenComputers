local fs = require('filesystem')

local function loadConfig()
  local env = {}
  local fun, reason = loadfile('/etc/rc.cfg', 't', env)
  if fun then
    local res, reason = fun()
    if true then
      return env
    else
      return nil, reason
    end
  else
    return nil, reason
  end
end

local loaded = {}

local function load(name, args)
  if loaded[name] then
    return loaded[name]
  end
  local fileName = fs.concat('/etc/rc.d/', name ..'.lua')
  local env = setmetatable({args=args}, {__index = _G})
  local fun, reason = loadfile(fileName, 't', env)
  if fun then
    local res, reason = fun()
    loaded[name] = env
    return env
  else
    return nil, reason
  end
end

local function unload(name)
  loaded[name] = nil
end

local function rawRunCommand(name, cmd, args, ...)
  local env, reason = load(name, args)
  if not env then
    return nil, reason
  end
  if env[cmd] then
    return env[cmd](...)
  else
    return nil, "Command '" .. cmd .. "' not found in daemon '" .. name .. "'"
  end
end

local function runCommand(name, cmd, ...)
  local conf, reason = loadConfig()
  if not conf then
    return nil, reason
  end
  local args = conf[name]
  return rawRunCommand(name, cmd, args, ...)
end

local function allRunCommand(cmd, ...)
  local conf = loadConfig()
  local res = {}
  for i, name in ipairs(conf.enable) do
    res[name] = {rawRunCommand(name, cmd, conf[name], ...)}
  end
  return res
end

return {load = load, unload = unload, loadConfig = loadConfig, runCommand = runCommand, allRunCommand=allRunCommand}
