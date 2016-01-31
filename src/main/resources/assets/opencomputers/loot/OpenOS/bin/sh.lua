local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

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
    else
      return nil -- eof
    end
  end
  if self.buffer == "" then
    self.args = table.pack(coroutine.yield(table.unpack(self.result)))
  end
  local result = string.sub(self.buffer, 1, n)
  self.buffer = string.sub(self.buffer, n + 1)
  return result
end

function memoryStream:write(value)
  if not self.redirect.write and self.closed then
    -- if next is dead, ignore all writes
    if coroutine.status(self.next) ~= "dead" then
      error("attempt to use a closed stream")
    end
    return true
  end
  if self.redirect.write then
    self.redirect.write:write(value)
  end
  if not self.closed then
    self.buffer = self.buffer .. value
    self.result = table.pack(coroutine.resume(self.next, table.unpack(self.args)))
    if coroutine.status(self.next) == "dead" then
      self:close()
    end
    if not self.result[1] then
      error(self.result[2], 0)
    end
    table.remove(self.result, 1)
  end
  return true
end

function memoryStream.new()
  local stream = {closed = false, buffer = "",
                  redirect = {}, result = {}, args = {}}
  local metatable = {__index = memoryStream,
                     __metatable = "memorystream"}
  return setmetatable(stream, metatable)
end

-------------------------------------------------------------------------------

local function expand(value)
  local result = value:gsub("%$(%w+)", os.getenv):gsub("%$%b{}",
    function(match) return os.getenv(expand(match:sub(3, -2))) or match end)
  return result
end

local function glob(value)
  if not value:find("*", 1, true) and not value:find("?", 1, true) then
    -- Nothing to do here.
    return {expand(value)}
  end
  local segments = fs.segments(value)
  local paths = {value:sub(1, 1) == "/" and "/" or shell.getWorkingDirectory()}
  for i, segment in ipairs(segments) do
    local nextPaths = {}
    local pattern = segment:gsub("*", ".*"):gsub("?", ".")
    if pattern == segment then
      -- Nothing to do, concatenate as-is.
      for _, path in ipairs(paths) do
        table.insert(nextPaths, fs.concat(path, segment))
      end
    else
      pattern = "^(" .. pattern .. ")/?$"
      for _, path in ipairs(paths) do
        for file in fs.list(path) do
          if file:match(pattern) then
            table.insert(nextPaths, fs.concat(path, file))
          end
        end
      end
      if #nextPaths == 0 then
        error("no matches found: " .. segment)
      end
    end
    paths = nextPaths
  end
  for i, path in ipairs(paths) do
    paths[i] = expand(path)
  end
  return paths
end

local function evaluate(value)
  local init, results = 1, {""}
  repeat
    local match = value:match("^%b''", init)
    if match then -- single quoted string. no variable expansion.
      match = match:sub(2, -2)
      init = init + 2
      for i, result in ipairs(results) do
        results[i] = result .. match
      end
    else
      match = value:match('^%b""', init)
      if match then -- double quoted string.
        match = match:sub(2, -2)
        init = init + 2
      else
        -- plaintext?
        match = value:match("^([^']+)%b''", init)
        if not match then -- unmatched single quote.
          match = value:match('^([^"]+)%b""', init)
          if not match then -- unmatched double quote.
            match = value:sub(init)
          end
        end
      end
      local newResults = {}
      for _, globbed in ipairs(glob(match)) do
        for i, result in ipairs(results) do
          table.insert(newResults, result .. globbed)
        end
      end
      results = newResults
    end
    init = init + #match
  until init > #value
  return results
end

local function parseCommand(tokens, ...)
  if #tokens == 0 then
    return
  end

  local program, args = shell.resolveAlias(tokens[1], table.pack(select(2, table.unpack(tokens))))

  local eargs = {}
  program = evaluate(program)
  for i = 2, #program do
    table.insert(eargs, program[i])
  end
  local program, reason = shell.resolve(program[1], "lua")
  if not program then
    return nil, reason
  end
  for i = 1, #args do
    for _, arg in ipairs(evaluate(args[i])) do
      table.insert(eargs, arg)
    end
  end
  args = eargs

  -- Find redirects.
  local input, output, mode = nil, nil, "write"
  tokens = args
  args = {}
  local function smt(call) -- state metatable factory
    local function index(_, token)
      if token == "<" or token == ">" or token == ">>" then
        return "parse error near " .. token
      end
      call(token)
      return "args" -- default, return to normal arg parsing
    end
    return {__index=index}
  end
  local sm = { -- state machine for redirect parsing
    args   = setmetatable({["<"]="input", [">"]="output", [">>"]="append"},
                              smt(function(token)
                                    table.insert(args, token)
                                  end)),
    input  = setmetatable({}, smt(function(token)
                                    input = token
                                  end)),
    output = setmetatable({}, smt(function(token)
                                    output = token
                                    mode = "write"
                                  end)),
    append = setmetatable({}, smt(function(token)
                                    output = token
                                    mode = "append"
                                  end))
  }
  -- Run state machine over tokens.
  local state = "args"
  for i = 1, #tokens do
    local token = tokens[i]
    state = sm[state][token]
    if not sm[state] then
      return nil, state
    end
  end
  return program, args, input, output, mode
end

local function parseCommands(command)
  local tokens, reason = text.tokenize(command)
  if not tokens then
    return nil, reason
  elseif #tokens == 0 then
    return true
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
  if #command > 0 then -- push tail command
    table.insert(commands, command)
  end

  for i = 1, #commands do
    commands[i] = table.pack(parseCommand(commands[i]))
    if commands[i][1] == nil then
      return nil, commands[i][2]
    end
  end

  return commands
end

-------------------------------------------------------------------------------

local function execute(env, command, ...)
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
    threads[i], reason = process.load(program, env, function()
      os.setenv("_", program)
      if input then
        local file, reason = io.open(shell.resolve(input))
        if not file then
          error("could not open '" .. input .. "': " .. reason, 0)
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
          error("could not open '" .. output .. "': " .. reason, 0)
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
    io.write('')
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
  for _, arg in ipairs(table.pack(...)) do
    table.insert(args, arg)
  end
  table.insert(args, 1, true)
  args.n = #args
  local result = nil
  for i = 1, #threads do
    -- Emulate CC behavior by making yields a filtered event.pull()
    while args[1] and coroutine.status(threads[i]) ~= "dead" do
      result = table.pack(coroutine.resume(threads[i], table.unpack(args, 2, args.n)))
      if coroutine.status(threads[i]) ~= "dead" then
        args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
      end
    end
    if pipes[i] then
      pcall(pipes[i].close, pipes[i])
    end
    if not result[1] then
      if type(result[2]) == "table" and result[2].reason == "terminated" then
        if result[2].code then
          result[1] = true
          result.n = 1
        else
          result[2] = "terminated"
        end
      elseif type(result[2]) == "string" then
        result[2] = debug.traceback(threads[i], result[2])
      end
      break
    end
  end

  -- copy env vars from last process; mostly to ensure stuff like cd.lua works
  local lastVars = rawget(process.info(threads[#threads]).data, "vars")
  if lastVars then
    local localVars = process.info().data.vars
    for k,v in pairs(lastVars) do
      localVars[k] = v
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

local args, options = shell.parse(...)
local history = {}

local function escapeMagic(text)
  return text:gsub('[%(%)%.%%%+%-%*%?%[%^%$]', '%%%1')
end

local function getMatchingPrograms(baseName)
  local result = {}
  -- TODO only matching files with .lua extension for now, might want to
  --      extend this to other extensions at some point? env var? file attrs?
  if not baseName or #baseName == 0 then
    baseName = "^(.*)%.lua$"
  else
    baseName = "^(" .. escapeMagic(baseName) .. ".*)%.lua$"
  end
  for basePath in string.gmatch(os.getenv("PATH"), "[^:]+") do
    for file in fs.list(basePath) do
      local match = file:match(baseName)
      if match then
        table.insert(result, match)
      end
    end
  end
  return result
end

local function getMatchingFiles(basePath, name)
  local resolvedPath = shell.resolve(basePath)
  local result, baseName = {}

  -- note: we strip the trailing / to make it easier to navigate through
  -- directories using tab completion (since entering the / will then serve
  -- as the intention to go into the currently hinted one).
  -- if we have a directory but no trailing slash there may be alternatives
  -- on the same level, so don't look inside that directory... (cont.)
  if fs.isDirectory(resolvedPath) and name:len() == 0 then
    baseName = "^(.-)/?$"
  else
    baseName = "^(" .. escapeMagic(name) .. ".-)/?$"
  end

  for file in fs.list(resolvedPath) do
    local match = file:match(baseName)
    if match then
      table.insert(result, basePath ..  match)
    end
  end
  -- (cont.) but if there's only one match and it's a directory, *then* we
  -- do want to add the trailing slash here.
  if #result == 1 and fs.isDirectory(result[1]) then
    result[1] = result[1] .. "/"
  end
  return result
end

local function hintHandler(line, cursor)
  local line = unicode.sub(line, 1, cursor - 1)
  if not line or #line < 1 then
    return nil
  end
  local result
  local prefix, partial = string.match(line, "^(.+%s)(.+)$")
  local searchInPath = not prefix and not line:find("/")
  if searchInPath then
    -- first part and no path, look for programs in the $PATH
    result = getMatchingPrograms(line)
  else -- just look normal files
    local partialPrefix = (partial or line)
    local name = partialPrefix:gsub("/+", "/")
    name = name:sub(-1) == '/' and '' or fs.name(name)
    partialPrefix = partialPrefix:sub(1, -name:len() - 1)
    result = getMatchingFiles(partialPrefix, name)
  end
  local resultSuffix = ""
  if searchInPath then
    resultSuffix  = " "
  elseif #result == 1 and result[1]:sub(-1) ~= '/' then
    resultSuffix = " "
  end
  prefix = prefix or ""
  for i = 1, #result do
    result[i] = prefix .. result[i] .. resultSuffix
  end
  table.sort(result)
  return result
end

if #args == 0 and (io.input() == io.stdin or options.i) and not options.c then
  -- interactive shell.
  while true do
    if not term.isAvailable() then -- don't clear unless we lost the term
      while not term.isAvailable() do
        event.pull("term_available")
      end
      term.clear()
    end
    while term.isAvailable() do
      local foreground = component.gpu.setForeground(0xFF0000)
      term.write(expand(os.getenv("PS1") or "$ "))
      component.gpu.setForeground(foreground)
      local command = term.read(history, nil, hintHandler)
      if not command then
        term.write("exit\n")
        return -- eof
      end
      while #history > (tonumber(os.getenv("HISTSIZE")) or 10) do
        table.remove(history, 1)
      end
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = os.execute(command)
        if term.getCursor() > 1 then
          term.write("\n")
        end
        if not result then
          io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
        end
      end
    end
  end
elseif #args == 0 and (io.input() ~= io.stdin) then
  while true do
    io.write(expand(os.getenv("PS1") or "$ "))
    local command = io.read("*l")
    if not command then
      io.write("exit\n")
    end
    command = text.trim(command)
    if command == "exit" then
      return
    elseif command ~= "" then
      local result, reason = os.execute(command)
      if not result then
        io.stderr:write((reason and tostring(reason) or "unknown error") .. "\n")
      end
    end
  end
else
  -- execute command.
  local result = table.pack(execute(...))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end
