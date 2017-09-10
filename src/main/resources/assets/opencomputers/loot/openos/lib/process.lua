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
  env = env or p.env
  local code
  if type(path) == "string" then
    code = function(...)
      local fs, shell = require("filesystem"), require("shell")
      local program, reason = shell.resolve(path, "lua")
      if not program then
        return require("tools/programLocations").reportNotFound(path, reason)
      end
      os.setenv("_", program)
      local f = fs.open(program)
      if f then
        local shebang = (f:read(1024) or ""):match("^#!([^\n]+)")
        f:close()
        if shebang then
          path = shebang:gsub("%s","")
          return code(program, ...)
        end
      end
      -- local command
      return assert(loadfile(program, "bt", env))(...)
    end
  else -- path is code
    code = path
  end

  local thread = nil
  thread = coroutine.create(function(...)
    -- pcall code so that we can remove it from the process list on exit
    local result =
    {
      xpcall(function(...)
          init = init or function(...) return ... end
          return code(init(...))
        end,
        function(msg)
          -- msg can be a custom error object
          if type(msg) == "table" then
            if msg.reason ~= "terminated" then
              io.stderr:write(tostring(msg.reason), "\n")
            end
            return msg.code or 0
          end
          local stack = debug.traceback():gsub("^([^\n]*\n)[^\n]*\n[^\n]*\n","%1")
          io.stderr:write(string.format("%s:\n%s", msg or "", stack))
          return 128 -- syserr
        end, ...)
    }
    process.internal.close(thread, result)
    --result[1] is false if the exception handler also crashed
    if not result[1] and type(result[2]) ~= "number" then
      require("event").onError(string.format("process library exception handler crashed: %s", tostring(result[2])))
    end
    return select(2, table.unpack(result))
  end, true)
  local new_proc =
  {
    path = path,
    command = name or tostring(path),
    env = env,
    data =
    {
      handles = {},
      io = {},
    },
    parent = p,
    instances = setmetatable({}, {__mode="v"}),
  }
  setmetatable(new_proc.data.io, {__index=p.data.io})
  setmetatable(new_proc.data, {__index=p.data})
  process.list[thread] = new_proc

  return thread
end

function process.info(levelOrThread)
  checkArg(1, levelOrThread, "thread", "number", "nil")
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
function process.internal.close(thread, result)
  checkArg(1,thread,"thread")
  local pdata = process.info(thread).data
  pdata.result = result
  for _,v in pairs(pdata.handles) do
    pcall(v.close, v)
  end
  process.list[thread] = nil
end

function process.internal.continue(co, ...)
  local result = {}
  -- Emulate CC behavior by making yields a filtered event.pull()
  local args = table.pack(...)
  while coroutine.status(co) ~= "dead" do
    result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    if coroutine.status(co) ~= "dead" then
      args = table.pack(coroutine.yield(table.unpack(result, 2, result.n)))
    elseif not result[1] then
      io.stderr:write(result[2])
    end
  end
  return table.unpack(result, 2, result.n)
end

function process.running(level) -- kept for backwards compat, prefer process.info
  local info = process.info(level)
  if info then
    return info.path, info.env, info.command
  end
end

return process
