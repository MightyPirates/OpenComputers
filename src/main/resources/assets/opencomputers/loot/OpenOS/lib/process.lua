local process = {}

-------------------------------------------------------------------------------

local running = setmetatable({}, {__mode="k"})
local coroutine_create = coroutine.create

local function findProcess(co)
  co = co or coroutine.running()
  for _, process in pairs(running) do
    for _, instance in pairs(process.instances) do
      if instance == co then
        return process
      end
    end
  end
end

-------------------------------------------------------------------------------

function process.load(path, env, init, name)
  checkArg(1, path, "string")
  checkArg(2, env, "table", "nil")
  checkArg(3, init, "function", "nil")
  checkArg(4, name, "string", "nil")

  local process = findProcess()
  if process then
    env = env or process.env
  end
  env = setmetatable({}, {__index=env or _G})
  local f, reason = io.open(path)
  if not f then
    return nil, reason
  end
  local code, reason
  if f:read(2) == "#!" then
    local command = f:read()
    if require("text").trim(command) == "" then
      reason = "no exec command"
    else
      code = function()
        return require("shell").execute(command, env, path)
      end
    end
  else
    code, reason = loadfile(path, "t", env)
  end
  f:close()
  if not code then
    return nil, reason
  end

  local thread = coroutine_create(function(...)
    if init then
      init()
    end
    return code(...)
  end)
  running[thread] = {
    path = path,
    command = name,
    env = env,
    parent = process,
    instances = setmetatable({thread}, {__mode="v"})
  }
  return thread
end

function process.running(level)
  level = level or 1
  local process = findProcess()
  while level > 1 and process do
    process = process.parent
    level = level - 1
  end
  if process then
    return process.path, process.env, process.command
  end
end

function process.install(path, name)
  _G.coroutine.create = function(f)
    local co = coroutine_create(f)
    table.insert(findProcess().instances, co)
    return co
  end
  local load = load
  _G.load = function(ld, source, mode, env)
    env = env or select(2, process.running())
    return load(ld, source, mode, env)
  end
  local thread = coroutine.running()
  running[thread] = {
    path = path,
    command = name,
    env = _ENV,
    instances = setmetatable({thread}, {__mode="v"})
  }
end

return process