-- BEtter SHell, a wrapper for the normal shell that adds many POSIX
-- features, such as pipes, redirects and variable expansion.

local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

local function expandVars(token)
  local name = nil
  local special = false
  local ignore = false
  local ignoreChar =''
  local escaped = false
  local lastEnd = 1
  local doubleQuote = false
  local singleQuote = false
  local endToken = {}
  for i = 1, unicode.len(token) do
    local char = unicode.sub(token, i, i)
    if escaped then
      if name then
        table.insert(name, char)
      end
      escaped = false
    elseif char == '\\' then
      escaped = not escaped
      table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      lastEnd = i+1
    elseif char == '"' and not singleQuote then
      doubleQuote = not doubleQuote
      table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      lastEnd = i+1
    elseif char == "'" and not doubleQuote then
      singleQuote = not singleQuote
      table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      lastEnd = i+1
    elseif char == "$" and not doubleQuote and not singleQuote then
      if name then
        ignore = true
      else
        name = {}
        table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      end
    elseif char == '{' and #name == 0 then
      if ignore and ignoreChar == '' then
        ignoreChar = '}'
      else
        special = true
      end
    elseif char == '(' and ignoreChar == '' then
      ignoreChar = ')'
    elseif char == '`' and special then
      ignore = true
      ignoreChar = '`'
    elseif char == '}' and not ignore and not doubleQuote and not singleQuote then
      table.insert(endToken, os.getenv(table.concat(name)))
      name = nil
      lastEnd = i+1
    elseif char == '"' and not singleQuote then
      doubleQuote = not doubleQuote
    elseif char == "'" and not doubleQuote then
      singleQuote = not singleQuote
    elseif name and (char:match("[%a%d_]") or special) then
      if char:match("%d") and #name == 0 then
        error "Identifiers can't start with a digit!"
      end
      table.insert(name, char)
    elseif char == ignoreChar and ignore then
      ignore = false
      ignoreChar = ''
    elseif name then -- We are done with gathering the name
      table.insert(endToken, os.getenv(table.concat(name)))
      name = nil
      lastEnd = i
    end
  end
  if name then
    table.insert(endToken, os.getenv(table.concat(name)))
    name = nil
  else
    table.insert(endToken, unicode.sub(token, lastEnd, -1))
  end
  return table.concat(endToken)
end

local function parseCommand(tokens)
  if #tokens == 0 then
    return
  end

  -- Variable expansion for all command parts.
  for i = 1, #tokens do
    tokens[i] = expandVars(tokens[i])
  end

  -- Resolve alias for command.
  local program, args = shell.resolveAlias(tokens[1], table.pack(select(2, table.unpack(tokens))))

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
    if commands[i][1] == nil then
      return nil, commands[i][2]
    end
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

local function execute(command, env, ...)
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
    threads[i], reason = process.load(shell.resolve(program, "lua"), env, function()
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
  if not result[1] and type(result[2]) == "table" and result[2].reason == "terminated" then
    if result[2].code then
      return true
    else
      return false, "terminated"
    end
  end
  return table.unpack(result, 1, result.n)
end

local env = setmetatable({os=setmetatable({execute=execute}, {__index=os})}, {__index=_ENV})
shell.execute("/bin/sh", env, ...)