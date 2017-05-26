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
  name = name or ""

  local p = process.findProcess()
  env = env or p.env
  local code
  if type(path) == 'string' then
    code = function(...)
      local fs, shell = require("filesystem"), require("shell")
      local program, reason = shell.resolve(path, 'lua')
      if not program then
        if fs.isDirectory(shell.resolve(path)) then
          io.stderr:write(path .. ": is a directory\n")
          return 126
        end
        local handler = require("tools/programLocations")
        handler.reportNotFound(path, reason)
        return 127
      end
      os.setenv("_", program)
      local f = fs.open(program)
      if f then
        local shebang = f:read(1024):match("^#!([^\n]+)")
        f:close()
        if shebang then
          local result = table.pack(shell.execute(shebang:gsub("%s",""), env, program, ...))
          assert(result[1], result[2])
          return table.unpack(result)
        end
      end
      local command
      command, reason = loadfile(program, "bt", env)
      if not command then
        io.stderr:write(program..(reason or ""):gsub("^[^:]*", "").."\n")
        return 128
      end
      return command(...)
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
          os.setenv("_", name)
          init = init or function(...) return ... end
          return code(init(...))
        end,
        function(msg)
          -- msg can be a custom error object
          if type(msg) == 'table' then
            if msg.reason ~= "terminated" then
              io.stderr:write(msg.reason.."\n")
            end
            return msg.code or 0
          end
          local stack = debug.traceback():gsub('^([^\n]*\n)[^\n]*\n[^\n]*\n','%1')
          io.stderr:write(string.format('%s:\n%s', msg or '', stack))
          return 128 -- syserr
        end, ...)
    }
    process.internal.close(thread)
    --result[1] is false if the exception handler also crashed
    if not result[1] and type(result[2]) ~= "number" then
      require("event").onError(string.format("process library exception handler crashed: %s", tostring(result[2])))
    end
    return select(2, table.unpack(result))
  end, true)
  local new_proc =
  {
    path = path,
    command = name,
    env = env,
    data =
    {
      handles = {},
      io = {},
      coroutine_handler = {}
    },
    parent = p,
    instances = setmetatable({}, {__mode="v"}),
  }
  setmetatable(new_proc.data.io, {__index=p.data.io})
  setmetatable(new_proc.data.coroutine_handler, {__index=p.data.coroutine_handler})
  setmetatable(new_proc.data, {__index=p.data})
  process.list[thread] = new_proc

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
