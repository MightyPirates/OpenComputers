local process = {}

-------------------------------------------------------------------------------

--Initialize coroutine library--
process.list = setmetatable({}, {__mode="k"})

function process.findProcess(co)
  co = co or coroutine.running()
  for main, p in pairs(process.list) do
    if main == co then
      return p
    end
    for _, instance in pairs(p.instances) do
      if instance == co then
        return p
      end
    end
  end
end

-------------------------------------------------------------------------------

function process.load(path, env, init, name)
  checkArg(1, path, "string", "function")
  checkArg(2, env, "table", "nil")
  checkArg(3, init, "function", "nil")
  checkArg(4, name, "string", "nil")

  assert(type(path) == "string" or env == nil, "process cannot load function environemnts")

  local p = process.findProcess()
  if p then
    env = env or p.env
  end
  env = setmetatable({}, {__index=env or _G})

  local code = nil
  if type(path) == 'string' then
    local f, reason = io.open(path)
    if not f then
      return nil, reason
    end
    local reason
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
  else -- path is code
    code = path
  end

  local thread = nil
  thread = coroutine.create(function(...)
    if init then
      init()
    end
    -- pcall code so that we can remove it from the process list on exit
    local result = 
    {
      xpcall(code, function(msg)
        if type(msg) == 'table' then return msg end
        local stack = debug.traceback():gsub('^([^\n]*\n)[^\n]*\n[^\n]*\n','%1')
        return string.format('%s:\n%s', msg or '', stack)
      end, ...)
    }
    process.internal.close(thread)
    if not result[1] then
      -- msg can be a custom error object
      local msg = result[2]
      if type(msg) == 'table' then
        if msg.reason~="terminated" then error(msg.reason,2) end
        result={0,msg.code}
      else
        error(msg,2)
      end
    end
    return select(2,table.unpack(result))
  end,true)
  process.list[thread] = {
    path = path,
    command = name,
    env = env,
    data = setmetatable(
    {
      handles = {},
      io = setmetatable({}, {__index=p and p.data and p.data.io or nil}),
      coroutine_handler = setmetatable({}, {__index=p and p.data and p.data.coroutine_handler or nil}),
    }, {__index=p and p.data or nil}),
    parent = p,
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
  local p
  if type(levelOrThread) == "thread" then
    p = process.findProcess(levelOrThread)
  else
    local level = levelOrThread or 1
    p = process.findProcess()
    while level > 1 and p do
      p = p.parent
      level = level - 1
    end
  end
  if p then
    return {path=p.path, env=p.env, command=p.command, data=p.data}
  end
end

--table of undocumented api subject to change and intended for internal use
process.internal = {}
--this is a future stub for a more complete method to kill a process
function process.internal.close(thread)
  checkArg(1,thread,"thread")
  local pdata = process.info(thread).data
  for k,v in pairs(pdata.handles) do
    v:close()
  end
  process.list[thread] = nil
end

return process
