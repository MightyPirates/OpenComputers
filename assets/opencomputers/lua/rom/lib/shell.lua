local cwd = "/"
local path = {"/bin/", "/usr/bin/", "/home/bin/"}
local aliases = {dir="ls", move="mv", rename="mv", copy="cp", del="rm",
                 md="mkdir", cls="clear"}

local function onTermAvailable()
  term.clear()
  print("OpenOS v1.0 (" .. math.floor(os.totalMemory() / 1024) .. "k RAM)")
  while term.isAvailable() do
    io.write("> ")
    local command = io.read()
    if not command then
      return -- eof
    end
    local result, reason = os.execute(command)
    if not result then
      print(reason)
    end
  end
end

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

  -- Track listeners and timers registered by spawned programs so we can kill
  -- them all when the coroutine dies. Note that this is only intended as a
  -- convenience, and is easily circumvented (e.g. by using dofile or such).
  local listeners, weakListeners, timers = {}, {}, {}
  local pevent = {}
  function pevent.ignore(name, callback, weak)
    local list
    if weak then
      if weakListeners[name] and weakListeners[name][callback] then
        list = weakListeners
      end
    elseif listeners[name] and listeners[name][callback] then
      list = listeners
    end
    if list then
      event.ignore(name, callback)
      list[name][callback] = nil
      return true
    end
    return false
  end
  function pevent.listen(name, callback, weak)
    if event.listen(name, callback, weak) then
      if weak then
        weakListeners[name] = weakListeners[name] or {}
        weakListeners[name][callback] = true
      else
        listeners[name] = listeners[name] or {}
        listeners[name][callback] = nil
      end
      return true
    end
    return false
  end
  function pevent.cancel(timerId)
    if timers[timerId] then
      timers[timerId] = nil
      return event.cancel(timerId)
    end
    return false
  end
  function pevent.timer(timeout, callback)
    local id
    local function onTimer()
      timers[id] = nil
      callback()
    end
    id = event.timer(timeout, onTimer)
    timers[id] = true
    return id
  end
  function pevent.interval(timeout, callback)
    local interval = {}
    local function onTimer()
      interval.id = pevent.timer(timeout, onTimer)
      callback()
    end
    interval.id = pevent.timer(timeout, onTimer)
    return interval
  end
  pevent = setmetatable(pevent, {__index = event, __metatable = {}})
  local env = setmetatable({event = pevent}, {__index = _ENV, __metatable = {}})

  program, reason = loadfile(where, env)
  if not program then
    return nil, reason
  end

  event.ignore("term_available", onTermAvailable)
  local result = table.pack(pcall(program, ...))
  event.listen("term_available", onTermAvailable)

  for name, list in pairs(listeners) do
    for listener in pairs(list) do
      event.ignore(name, listener, false)
    end
  end
  for name, list in pairs(weakListeners) do
    for listener in pairs(list) do
      event.ignore(name, listener, true)
    end
  end
  for id in pairs(timers) do
    event.cancel(id)
  end

  return table.unpack(result, 1, result.n)
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

-------------------------------------------------------------------------------

os.execute = function(command)
  if not command then
    return type(shell) == "table"
  end
  checkArg(1, command, "string")
  local head, tail = nil, ""
  repeat
    local oldHead = head
    head = command:match("^%S+")
    tail = command:usub(head:ulen() + 1) .. tail
    if head == oldHead then -- say no to infinite recursion, live longer
      command = nil
    else
      command = shell.alias(head)
    end
  until command == nil
  local args = {}
  for part in tail:gmatch("%S+") do
    table.insert(args, part)
  end
  return shell.execute(head, table.unpack(args))
end

-------------------------------------------------------------------------------

return function()
  event.listen("term_available", onTermAvailable)
end
