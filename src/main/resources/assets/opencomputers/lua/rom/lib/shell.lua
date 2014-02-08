local event = require("event")
local fs = require("filesystem")
local unicode = require("unicode")
local text = require("text")

local shell = {}
local cwd = "/"
local path = {"/bin/", "/usr/bin/", "/home/bin/"}
local aliases = {}
local running = setmetatable({}, {__mode="k"})
local isLoading = false

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

local function resolveAlias(program, args)
  local lastProgram = nil
  while true do
    local alias = text.tokenize(shell.getAlias(program) or program)
    if alias[1] == lastProgram then
      break
    end
    lastProgram = program
    program = alias[1]
    if args then
      for i = 2, #alias do
        table.insert(args, alias[i])
      end
    end
  end
  return program
end

local function parseCommand(tokens)
  local program, args, input, output = tokens[1], {}, nil, nil
  if not program then
    return
  end

  program = resolveAlias(program, args)

  local state = "args"
  for i = 2, #tokens do
    if state == "args" then
      if tokens[i] == "<" then
        state = "input"
      elseif tokens[i] == ">" then
        state = "output"
      elseif tokens[i] == ">>" then
        state = "append"
      else
        table.insert(args, tokens[i])
      end
    elseif state == "input" then
      if tokens[i] == ">" then
        if not input then
          return nil, "parse error near '>'"
        end
        state = "output"
      elseif tokens[i] == ">>" then
        if not input then
          return nil, "parse error near '>>'"
        end
        state = "append"
      elseif not input then
        input = tokens[i]
      end
    elseif state == "output" or state == "append" then
      if not output then
        output = tokens[i]
      end
    end
  end
  return program, args, input, output, state
end

local function parseCommands(command)
  local tokens, reason = text.tokenize(command)
  if not tokens then
    return nil, reason
  end

  local commands, command = {}, {}
  for i = 1, #tokens do
    if tokens[i] == "|" then
      if #command == 0 then
        return nil, "parse error near '|'"
      end
      table.insert(commands, command)
      command = {}
    else
      table.insert(command, tokens[i])
    end
  end
  if #command > 0 then
    table.insert(commands, command)
  end

  for i = 1, #commands do
    commands[i] = table.pack(parseCommand(commands[i]))
  end

  return commands
end

-------------------------------------------------------------------------------

local memoryStream = {}

function memoryStream:close()
  self.closed = true
end

function memoryStream:seek()
  return nil, "bad file descriptor"
end

function memoryStream:read(n)
  if self.closed then
    if self.buffer == "" and self.redirect.read then
      return self.redirect.read:read(n)
    end
    return nil -- eof
  end
  if self.buffer == "" then
    self.args = table.pack(coroutine.yield(table.unpack(self.result)))
  end
  local result = string.sub(self.buffer, 1, n)
  self.buffer = string.sub(self.buffer, n + 1)
  return result
end

function memoryStream:write(value)
  local ok
  if self.redirect.write then
    ok = self.redirect.write:write(value)
  end
  if not self.closed then
    self.buffer = self.buffer .. value
    self.result = table.pack(coroutine.resume(self.next, table.unpack(self.args)))
    ok = true
  end
  if ok then
    return true
  end
  return nil, "stream is closed"
end

function memoryStream.new()
  local stream = {closed = false, buffer = "",
                  redirect = {}, result = {}, args = {}}
  local metatable = {__index = memoryStream,
                     __gc = memoryStream.close,
                     __metatable = "memorystream"}
  return setmetatable(stream, metatable)
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
  if dir == "//" then dir = "/" end
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

function shell.execute(command, env, ...)
  checkArg(1, command, "string")
  local commands, reason = parseCommands(command)
  if not commands then
    return false, reason
  end
  if #commands == 0 then
    return true
  end

  -- Piping data between programs works like so:
  -- program1 gets its output replaced with our custom stream.
  -- program2 gets its input replaced with our custom stream.
  -- repeat for all programs
  -- custom stream triggers execution of 'next' program after write.
  -- custom stream triggers yield before read if buffer is empty.
  -- custom stream may have 'redirect' entries for fallback/duplication.
  local threads, pipes, inputs, outputs = {}, {}, {}, {}
  for i = 1, #commands do
    local program, args, input, output, mode = table.unpack(commands[i])
    local reason
    threads[i], reason = shell.load(program, env, function()
      if input then
        local file, reason = io.open(shell.resolve(input))
        if not file then
          error(reason)
        end
        table.insert(inputs, file)
        if pipes[i - 1] then
          pipes[i - 1].stream.redirect.read = file
          io.input(pipes[i - 1])
        else
          io.input(file)
        end
      elseif pipes[i - 1] then
        io.input(pipes[i - 1])
      end
      if output then
        local file, reason = io.open(shell.resolve(output), mode == "append" and "a" or "w")
        if not file then
          error(reason)
        end
        if mode == "append" then
          io.write("\n")
        end
        table.insert(outputs, file)
        if pipes[i] then
          pipes[i].stream.redirect.write = file
          io.output(pipes[i])
        else
          io.output(file)
        end
      elseif pipes[i] then
        io.output(pipes[i])
      end
    end, command)
    if not threads[i] then
      return false, reason
    end

    if i < #commands then
      pipes[i] = require("buffer").new("rw", memoryStream.new())
      pipes[i]:setvbuf("no")
    end
    if i > 1 then
      pipes[i - 1].stream.next = threads[i]
      pipes[i - 1].stream.args = args
    end
  end

  local args = select(2, table.unpack(commands[1]))
  table.insert(args, 1, true)
  for _, arg in ipairs(table.pack(...)) do
    table.insert(args, arg)
  end
  args.n = #args
  local result = nil
  for i = 1, #threads do
    -- Emulate CC behavior by making yields a filtered event.pull()
    while args[1] and coroutine.status(threads[i]) ~= "dead" do
      result = table.pack(coroutine.resume(threads[i], table.unpack(args, 2, args.n)))
      if coroutine.status(threads[i]) ~= "dead" then
        if type(result[2]) == "string" then
          args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
        else
          args = {true, n=1}
        end
      end
    end
    if pipes[i] then
      pipes[i]:close()
    end
    if i < #threads and not result[1] then
      io.write(result[2])
    end
  end
  for _, input in ipairs(inputs) do
    input:close()
  end
  for _, output in ipairs(outputs) do
    output:close()
  end
  if not args[1] then
    return false, args[2]
  end
  return table.unpack(result, 1, result.n)
end

function shell.load(path, env, init, name)
  checkArg(1, path, "string")
  checkArg(2, env, "table", "nil")
  checkArg(3, init, "function", "nil")
  checkArg(4, name, "string", "nil")
  local filename, reason = shell.resolve(path, "lua")
  if not filename then
    return nil, reason
  end

  local process = findProcess()
  if process then
    env = env or process.env
  end
  env = setmetatable({}, {__index=env or _ENV})
  local code, reason = loadfile(filename, "t", env)
  if not code then
    return nil, reason
  end

  isLoading = true
  local thread = coroutine.create(function(...)
    if init then
      init()
    end
    return code(...)
  end)
  isLoading = false
  running[thread] = {
    path = filename,
    command = name,
    env = env,
    parent = process,
    instances = setmetatable({thread}, {__mode="v"})
  }
  return thread
end

function shell.register(thread)
  checkArg(1, thread, "thread")
  if findProcess(thread) then
    return false -- already attached somewhere
  end
  if not isLoading then
    table.insert(findProcess().instances, thread)
  end
  return true
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
  local process = findProcess()
  while level > 1 and process do
    process = process.parent
    level = level - 1
  end
  if process then
    return process.path, process.env, process.command
  end
end

-------------------------------------------------------------------------------

return shell
