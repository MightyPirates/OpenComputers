local shell = {}
local cwd = "/"
local path = {"/bin/", "/usr/bin/", "/home/bin/"}
local aliases = {dir="ls", list="ls", move="mv", rename="mv", copy="cp",
                 del="rm", md="mkdir", cls="clear", more="less", rs="redstone",
                 view="edit -r"}
local running = {}

local function findFile(name, ext)
  checkArg(1, name, "string")
  local function findIn(dir)
    dir = fs.concat(fs.concat(dir, name), "..")
    name = fs.name(name)
    local list = fs.list(dir)
    if list then
      local files = {}
      for file in list do
        files[file] = true
      end
      if ext and unicode.sub(name, -(1 + unicode.len(ext))) == "." .. ext then
        if files[name] then
          return true, fs.concat(dir, name)
        end
      elseif files[name] then
        return true, fs.concat(dir, name)
      elseif ext then
        name = name .. "." .. ext
        if files[name] then
          return true, fs.concat(dir, name)
        end
      end
    end
    return false
  end
  if unicode.sub(name, 1, 1) == "/" then
    local found, where = findIn("/")
    if found then return where end
  else
    local found, where = findIn(shell.getWorkingDirectory())
    if found then return where end
    for _, p in ipairs(path) do
      local found, where = findIn(p)
      if found then return where end
    end
  end
  return false
end

-------------------------------------------------------------------------------

function shell.getAlias(alias)
  return aliases[alias]
end

function shell.setAlias(alias, value)
  checkArg(1, alias, "string")
  checkArg(2, value, "string", "nil")
  aliases[alias] = value
end

function shell.aliases()
  return pairs(aliases)
end

function shell.getWorkingDirectory()
  return cwd
end

function shell.setWorkingDirectory(dir)
  checkArg(1, dir, "string")
  dir = fs.canonical(dir) .. "/"
  if fs.isDirectory(dir) then
    cwd = dir
    return true
  else
    return nil, "not a directory"
  end
end

function shell.getPath()
  return table.concat(path, ":")
end

function shell.setPath(value)
  checkArg(1, value, "string")
  path = {}
  for p in string.gmatch(value, "[^:]+") do
    p = fs.canonical(text.trim(p))
    if unicode.sub(p, 1, 1) ~= "/" then
      p = "/" .. p
    end
    table.insert(path, p)
  end
end

function shell.resolve(path, ext)
  if ext then
    checkArg(2, ext, "string")
    local where = findFile(path, ext)
    if where then
      return where
    else
      return nil, "file not found"
    end
  else
    if unicode.sub(path, 1, 1) == "/" then
      return fs.canonical(path)
    else
      return fs.concat(shell.getWorkingDirectory(), path)
    end
  end
end

function shell.execute(program, env, ...)
  checkArg(1, program, "string")
  local filename, reason = shell.resolve(program, "lua")
  if not filename then
    return nil, reason
  end
  local code, reason = loadfile(filename, "t", setmetatable({}, {__index=env}))
  if not code then
    return nil, reason
  end
  local co, args, result = coroutine.create(code), table.pack(true, ...), nil
  table.insert(running, filename)
  -- Emulate CC behavior by making yields a filtered event.pull()
  repeat
    result = table.pack(coroutine.resume(co, table.unpack(args, 2, args.n)))
    if coroutine.status(co) == "dead" then
      break
    end
    if type(result[2]) == "string" then
      args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
    else
      args = {true, n=1}
    end
  until not args[1]
  table.remove(running)
  if not args[1] then
    return false, args[2]
  end
  return table.unpack(result, 1, result.n)
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

function shell.running(level)
  level = level or 1
  return running[1 + (#running - level)]
end

-------------------------------------------------------------------------------

_G.shell = shell
