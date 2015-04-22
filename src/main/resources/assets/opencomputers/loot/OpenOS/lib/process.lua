local process = {}

-------------------------------------------------------------------------------

local running = setmetatable({}, {__mode="k"})
local coroutine_create = coroutine.create

local function findProcess(co)
  co = co or coroutine.running()
  for main, process in pairs(running) do
    if main == co then
      return process
    end
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
        local result = table.pack(require("shell").execute(command, env, path))
        if not result[1] then
          error(result[2], 0)
        else
          return table.unpack(result, 1, result.n)
        end
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
    data = setmetatable({}, {__index=process and process.data or nil}),
    parent = process,
    instances = setmetatable({}, {__mode="v"})
  }
  return thread
end

function process.running(level) -- kept for backwards compat, prefer process.info
  local info = process.info(level)
  if info then
    return info.path, info.env, info.command
  end
end

function process.info(levelOrThread)
  local process
  if type(levelOrThread) == "thread" then
    process = findProcess(levelOrThread)
  else
    local level = levelOrThread or 1
    process = findProcess()
    while level > 1 and process do
      process = process.parent
      level = level - 1
    end
  end
  if process then
    return {path=process.path, env=process.env, command=process.command, data=process.data}
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
    data = {},
    instances = setmetatable({}, {__mode="v"})
  }
end

return process