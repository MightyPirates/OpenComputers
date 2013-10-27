local cwd = "/"
local path = {"/bin/", "/usr/bin/", "/home/bin/"}
local aliases = {dir="ls", move="mv", rename="mv", copy="cp", del="rm",
                 md="mkdir", cls="clear", more="less", rs="redstone"}

local function findFile(name, path, ext)
  checkArg(1, name, "string")
  local function findIn(path)
    path = fs.concat(fs.concat(path, name), "..")
    name = fs.name(name)
    local list = fs.list(path)
    if list then
      local files = {}
      for file in list do
        files[file] = true
      end
      if ext and name:usub(-(1 + ext:ulen())) == "." .. ext then
        if files[name] then
          return true, fs.concat(path, name)
        end
      elseif files[name] then
        return true, fs.concat(path, name)
      elseif ext then
        name = name .. "." .. ext
        if files[name] then
          return true, fs.concat(path, name)
        end
      end
    end
    return false
  end
  if name:usub(1, 1) == "/" then
    local found, where = findIn("/")
    if found then return where end
  else
    local found, where = findIn(shell.cwd())
    if found then return where end
    if path then
      for _, p in ipairs(path) do
        local found, where = findIn(p)
        if found then return where end
      end
    end
  end
  return false
end

-------------------------------------------------------------------------------
-- We pseudo-sandbox programs we start via the shell. Pseudo because it's
-- really just a matter of convenience: listeners and timers get automatically
-- cleaned up when the program exits/crashes. This can be easily circumvented
-- by getting the parent environment via `getmetatable(_ENV).__index`. But if
-- you do that you will probably know what you're doing.
function newEnvironment()
  local listeners, timers, e = {[false]={}, [true]={}}, {}, {}
  local env = setmetatable(e, {__index=_ENV})

  e._G = e
  e.event = {}
  function e.event.ignore(name, callback, weak)
    weak = weak or false
    if listeners[weak][name] and listeners[weak][name][callback] then
      listeners[weak][name][callback] = nil
      return event.ignore(name, callback, weak)
    end
    return false
  end
  function e.event.listen(name, callback, weak)
    weak = weak or false
    if event.listen(name, callback, weak) then
      listeners[weak][name] = listeners[weak][name] or {}
      listeners[weak][name][callback] = true
      return true
    end
    return false
  end
  function e.event.cancel(timerId)
    if timers[timerId] then
      timers[timerId] = nil
      return event.cancel(timerId)
    end
    return false
  end
  function e.event.timer(timeout, callback)
    local id
    local function onTimer()
      timers[id] = nil
      callback()
    end
    id = event.timer(timeout, onTimer)
    timers[id] = true
    return id
  end
  function e.event.interval(frequency, callback)
    local interval = {}
    local function onTimer()
      interval.id = env.event.timer(frequency, onTimer)
      callback()
    end
    interval.id = env.event.timer(frequency, onTimer)
    return interval
  end
  setmetatable(e.event, {__index=event, __newindex=event})

  function e.load(ld, source, mode, environment)
    return load(ld, source, mode, environment or env)
  end
  function e.loadfile(filename, mode, environment)
    return loadfile(filename, mode, environment or env)
  end
  function e.dofile(filename)
    local program, reason = env.loadfile(filename)
    if not program then
      return env.error(reason, 0)
    end
    return program()
  end

  function cleanup()
    for weak, list in pairs(listeners) do
      for name, callbacks in pairs(list) do
        for callback in pairs(callbacks) do
          event.ignore(name, callback, weak)
        end
      end
    end
    for id in pairs(timers) do
      event.cancel(id)
    end
  end

  return env, cleanup
end

-------------------------------------------------------------------------------

shell = {}

function shell.alias(alias, ...)
  checkArg(1, alias, "string")
  local result = aliases[alias]
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(2, args[1], "string", "nil")
    aliases[alias] = args[1]
  end
  return result
end

function shell.aliases()
  return pairs(aliases)
end

function shell.cwd(path)
  if path then
    checkArg(1, path, "string")
    local path = fs.canonical(path) .. "/"
    if fs.isDirectory(path) then
      cwd = path
    else
      return nil, "not a directory"
    end
  end
  return cwd
end

function shell.execute(program, ...)
  local where = findFile(program, path, "lua")
  if not where then
    return nil, "program not found"
  end
  local env, cleanup = newEnvironment()
  program, reason = loadfile(where, "t", env)
  if not program then
    return nil, reason
  end
  local result = table.pack(pcall(program, ...))
  cleanup()
  return table.unpack(result, 1, result.n)
end

function shell.parse(...)
  local params = table.pack(...)
  local args = {}
  local options = {}
  for i = 1, params.n do
    local param = params[i]
    if param:usub(1, 1) == "-" then
      for j = 2, param:ulen() do
        options[param:usub(j, j)] = true
      end
    else
      table.insert(args, param)
    end
  end
  return args, options
end

function shell.path(...)
  local result = table.concat(path, ":")
  local args = table.pack(...)
  if args.n > 0 then
    checkArg(1, args[1], "string")
    path = {}
    for p in string:gmatch(args[1], "[^:]") do
      p = fs.canonical(string.trim(p))
      if p:usub(1, 1) ~= "/" then
        p = "/" .. p
      end
      table.insert(path, p)
    end
  end
  return result
end

function shell.resolve(path)
  if path:usub(1, 1) == "/" then
    return fs.canonical(path)
  else
    return fs.concat(shell.cwd(), path)
  end
end

function shell.which(program)
  local where = findFile(program, path, "lua")
  if where then
    return where
  else
    return nil, "program not found"
  end
end
