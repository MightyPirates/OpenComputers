local cwd = "/"
local path = {"/bin/", "/usr/bin/", "/home/bin/"}
local aliases = {dir="ls", move="mv", rename="mv", copy="cp", del="rm",
                 md="mkdir", cls="clear", more="less", rs="redstone"}
local running = {}

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
      if ext and unicode.sub(name, -(1 + unicode.len(ext))) == "." .. ext then
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
  if unicode.sub(name, 1, 1) == "/" then
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
  if type(program) ~= "function" then
    local where = findFile(program, path, "lua")
    if not where then
      return nil, "program not found"
    end
    local code, reason = loadfile(where, "t", setmetatable({}, {__index=_ENV}))
    if not code then
      return nil, reason
    end
    program = code
  end
  local co, args, result = coroutine.create(program), table.pack(true, ...), nil
  running[co] = true
  while running[co] and args[1] do
    result = table.pack(coroutine.resume(co, table.unpack(args, 2, args.n)))
    if coroutine.status(co) ~= "dead" then
      -- Emulate CC behavior by making yields a filtered event.wait.
      if type(result[2]) == "string" then
        args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
      else
        args = {true, n=1}
      end
    else
      break
    end
  end
  running[co] = nil
  if not args[1] then
    return false, "interrupted"
  end
  return table.unpack(result, 1, result.n)
end

function shell.kill(co)
  if running[co] ~= nil then
    running[co] = nil
    return true
  end
  return nil, "not a program thread"
end

function shell.parse(...)
  local params = table.pack(...)
  local args = {}
  local options = {}
  for i = 1, params.n do
    local param = params[i]
    if unicode.sub(param, 1, 1) == "-" then
      for j = 2, unicode.len(param) do
        options[unicode.sub(param, j, j)] = true
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
      if unicode.sub(p, 1, 1) ~= "/" then
        p = "/" .. p
      end
      table.insert(path, p)
    end
  end
  return result
end

function shell.resolve(path)
  if unicode.sub(path, 1, 1) == "/" then
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
