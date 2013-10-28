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
  local where = findFile(program, path, "lua")
  if not where then
    return nil, "program not found"
  end
  program, reason = loadfile(where, "t", setmetatable({}, {__index=_ENV}))
  if not program then
    return nil, reason
  end
  local args, co, result = table.pack(...), coroutine.create(program)
  repeat
    result = table.pack(coroutine.resume(co, table.unpack(args, 1, args.n)))
    if coroutine.status(co) ~= "dead" then
      args = table.pack(event.wait(result[2])) -- emulate CC behavior
    end
  until coroutine.status(co) == "dead"
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
