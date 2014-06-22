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

-- Avoid scoping issues by simple forward declaring all methods... it's
-- not a beauty, but it works :P
local expand, expandCmd, expandMath, expandParam, parseCommand, parseCommands

expandParam = function(param)
  local par, word, op = nil, nil, nil
  for _, oper in ipairs{':%-', '%-', ':=', '=', ':%?','%?', ':%+', '%+'} do
    par, word = param:match("(.-)"..oper.."(.*)")
    if word then
      op = oper
      break
    end
  end
  if word then
    local stat = os.getenv(par)
    if op == ':%-' then
      if stat ~= '' and stat ~= nil then
        return stat
      else
        return word
      end
    elseif op == '%-' then
      if stat ~= '' and stat ~= nil then
        return stat
      elseif stat == '' then
        return nil
      elseif stat == nil then
        return expand(word)
      end
    elseif op == ':=' then
      if stat ~= '' and stat ~= nil then
        return stat
      else
        os.setenv(par, word)
        return expand(word)
      end
    elseif op == '=' then
      if stat ~= '' and stat ~= nil then
        return stat
      elseif stat == '' then
        return nil
      elseif stat == nil then
        os.setenv(par, word)
        return expand(word)
      end
    elseif op == ':%?' then
      if stat ~= '' and stat ~= nil then
        return stat
      else
        error(par.." is not set!")
      end
    elseif op == '%?' then
      if stat ~= '' and stat ~= nil then
        return stat
      elseif stat == '' then
        return nil
      elseif stat == nil then
        error(par.." is not set")
      end
    elseif op == ':%+' then
      if stat ~= '' and stat ~= nil then
        return expand(word)
      else
        return nil
      end
    elseif op == '%+' then
      if stat ~= nil then
        return expand(word)
      else
        return nil
      end
    end
  elseif string.sub(param, 1,1) == '#' then
    return #(os.getenv(param:sub(2, -1)))
  else
    return os.getenv(param)
  end
end

expandCmd = function(cmd)
  return cmd
end

expandMath = function(expr)
  local success, reason = load("return "..expr, os.getenv("SHELL"), 't', {})
  if success then
    return success()
  else
    return reason
  end
end

expand = function(token)
  local expr = {}
  local matchStack = {}
  local escaped = false
  local lastEnd = 1
  local doubleQuote = false
  local singleQuote = false
  local mathBoth
  local endToken = {}
  for i = 1, unicode.len(token) do
    local char = unicode.sub(token, i, i)
    if escaped then
      if expr then
        table.insert(expr, char)
      end
      escaped = false
    elseif char == '\\' then
      escaped = not escaped
      table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      lastEnd = i+1
    elseif char == matchStack[#matchStack] then
      local match
      table.remove(matchStack)
      if char == '}' then
        local param = table.concat(table.remove(expr))
        match = expandParam(param)
      elseif char == ')' then
        if expr[#expr].cmd then
          local cmd = table.concat(table.remove(expr))
          match = expandCmd(cmd)
        elseif expr[#expr].math then
          if not mathBoth then
            mathBoth = i
          elseif mathBoth == i - 1 then
            local mth = table.concat(table.remove(expr))
            match = expandMath(mth)
            mathBoth = nil
          else
            return nil, "Unmatched )"
          end
        end
      elseif char == '`' then
        local cmd = table.concat(table.remove(expr))
        match = expandCmd(cmd)
      elseif char == "'" then
        singleQuote = false
      elseif char == '"' then
        doubleQuote = false
      end
      if #expr > 0 then
        table.insert(expr[#expr], match)
      else
        table.insert(endToken, match)
      end
      lastEnd = i+1
    elseif char == '"' and not singleQuote then
      doubleQuote = true
      if #expr <= 1 then
        table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      end
      table.insert(matchStack, '"')
      lastEnd = i+1
    elseif char == "'" and not doubleQuote then
      singleQuote = not singleQuote
      if #expr <= 0 then
        table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      end
      table.insert(matchStack, "'")
      lastEnd = i+1
    elseif char == "$" and not singleQuote then
      if #expr <= 0 then
        table.insert(endToken, unicode.sub(token, lastEnd, i-1))
      end
      table.insert(expr, {})
      lastEnd = i -1
    elseif char == '{' and #expr > 0 and #(expr[#expr]) == 0 then
      table.insert(matchStack, '}')
      if expr[#expr] == 0 then
        expr[#expr].special = true
      end
    elseif char == '(' and #expr > 0 and #(expr[#expr]) == 0 then
      table.insert(matchStack, ')')
      if expr[#expr].cmd then
        expr[#expr].cmd = false
        expr[#expr].math = true
      else
        expr[#expr].cmd = true
      end
    elseif char == '`' then
      table.insert(expr, {cmd = true})
      table.insert(matchStack, '`')
    elseif char == '"' and not singleQuote then
      doubleQuote = not doubleQuote
    elseif char == "'" and not doubleQuote then
      singleQuote = not singleQuote
    elseif #expr > 0 and (char:match("[%a%d_]") or #matchStack > 0) then
      table.insert(expr[#expr], char)
    elseif #expr > 0 then -- We are done with gathering the name
      table.insert(endToken, os.getenv(table.concat(table.remove(expr))))
      lastEnd = i
    end
  end
  while #expr > 0 do
    local xpr = table.remove(expr)
    table.insert(expr[#expr] or endToken, os.getenv(table.concat(xpr)))
    lastEnd = #token + 1
  end
  if lastEnd <= #token then
    table.insert(endToken, unicode.sub(token, lastEnd, -1))
  end
  return table.concat(endToken)
end

parseCommand = function(tokens)
  if #tokens == 0 then
    return
  end

  -- Variable expansion for all command parts.
  for i = 1, #tokens do
    tokens[i] = expand(tokens[i])
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

parseCommands = function(command)
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

local args, options = shell.parse(...)
local history = {}

if #args == 0 and (io.input() == io.stdin or options.i) and not options.c then
  -- interactive shell. use original shell for input but register self as
  -- global SHELL for command execution.
  local oldShell = os.getenv("SHELL")
  os.setenv("SHELL", process.running())
  os.execute("/bin/sh")
  os.setenv("SHELL", oldShell)
else
  -- execute command.
  local result = table.pack(execute(...))
  if not result[1] then
    error(result[2])
  end
  return table.unpack(result, 2)
end
