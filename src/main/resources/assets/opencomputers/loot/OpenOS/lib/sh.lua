local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")
local tx = require("transforms")
local unicode = require("unicode")

local sh = {}

sh.internal = setmetatable({},
{
  __tostring=function()
    return "table of undocumented api subject to change and intended for internal use"
  end
})

-- --[[@@]] are not just comments, but custom annotations for delayload methods.
-- See package.lua and the api wiki for more information
function isWordOf(w, vs) return w and #w == 1 and not w[1].qr and tx.first(vs,{{w[1].txt}}) ~= nil end
function isWord(w,v) return isWordOf(w,{v}) end
local local_env = {event=event,fs=fs,process=process,shell=shell,term=term,text=text,tx=tx,unicode=unicode,isWordOf=isWordOf,isWord=isWord}

-------------------------------------------------------------------------------

--SH API

sh.internal.globbers = {{"*",".*"},{"?","."}}
sh.internal.ec = {}
sh.internal.ec.parseCommand = 127
sh.internal.ec.sysError =  128
sh.internal.ec.last = 0

function sh.getLastExitCode()
  return sh.internal.ec.last
end

function sh.internal.command_passed(ec)
  local code = sh.internal.command_result_as_code(ec)
  return code == 0
end

function sh.internal.command_result_as_code(ec)
  -- convert lua result to bash ec
  if ec == false then
    return 1
  elseif ec == nil or ec == true or type(ec) ~= "number" then
    return 0
  else
    return ec
  end
end

function sh.internal.resolveActions(input, resolver, resolved)
  checkArg(1, input, "string")
  checkArg(2, resolver, "function", "nil")
  checkArg(3, resolved, "table", "nil")
  resolver = resolver or shell.getAlias
  resolved = resolved or {}

  local processed = {}

  local prev_was_delim, simple = true, true
  local words, reason = text.internal.tokenize(input)

  if not words then
    return nil, reason
  end

  while #words > 0 do
    local next = table.remove(words,1)
    if isWordOf(next, {";","&&","||","|"}) then
      prev_was_delim,simple = true,false
      resolved = {}
    elseif prev_was_delim then
      prev_was_delim = false
      -- if current is actionable, resolve, else pop until delim
      if next and #next == 1 and not next[1].qr then
        local key = next[1].txt
        if key == "!" then
          prev_was_delim,simple = true,false -- special redo
        elseif not resolved[key] then
          resolved[key] = resolver(key)
          local value = resolved[key]
          if value and key ~= value then
            local replacement_tokens, reason = sh.internal.resolveActions(value, resolver, resolved)
            if not replacement_tokens then
              return replacement_tokens, reason
            end
            simple = simple and reason
            words = tx.concat(replacement_tokens, words)
            next = table.remove(words,1)
          end
        end
      end
    end

    table.insert(processed, next)
  end

  return processed, simple
end

function sh.internal.statements(input)
  checkArg(1, input, "string")

  local words, reason = sh.internal.resolveActions(input)
  if type(words) ~= "table" then
    return words, reason
  elseif #words == 0 then
    return true
  elseif reason and not input:find("[<>]") then
    return {words}, reason
  end

  -- we shall validate pipes before any statement execution
  local statements = sh.internal.splitStatements(words)
  for i=1,#statements do
    local ok, why = sh.internal.hasValidPiping(statements[i])
    if not ok then return nil,why end
  end
  return statements
end

-- returns true if key is a string that represents a valid command line identifier
function sh.internal.isIdentifier(key)
  if type(key) ~= "string" then
    return false
  end

  return key:match("^[%a_][%w_]*$") == key
end

function sh.expand(value)
  return value
  :gsub("%$([_%w%?]+)", function(key)
    if key == "?" then
      return tostring(sh.getLastExitCode())
    end
    return os.getenv(key) or '' end)
  :gsub("%${(.*)}", function(key)
    if sh.internal.isIdentifier(key) then
      return sh.internal.expandKey(key)
    end
    error("${" .. key .. "}: bad substitution")
  end)
end

function sh.internal.expand(word)
  if #word == 0 then return {} end

  local result = ''
  for i=1,#word do
    local part = word[i]
    result = result .. (not (part.qr and part.qr[3]) and sh.expand(part.txt) or part.txt)
  end

  return {result}
end

-- expand to files in path, or try key substitution
-- word is a list of metadata-filled word parts
-- note: text.internal.words(string) returns an array of these words
function sh.internal.evaluate(word)
  checkArg(1, word, "table")
  if #word == 0 then
    return {}
  elseif #word == 1 and word[1].qr then
    return sh.internal.expand(word)
  end
  local function make_pattern(seg)
    local result = seg
    for _,glob_rule in ipairs(sh.internal.globbers) do
      result = result:gsub("%%%"..glob_rule[1], glob_rule[2])
      local reduced = result
      repeat
        result = reduced
        reduced = result:gsub(text.escapeMagic(glob_rule[2]):rep(2), glob_rule[2])
      until reduced == result
    end
    return result
  end
  local glob_pattern = ''
  local has_globits = false
  for i=1,#word do local part = word[i]
    local next = part.txt
    if not part.qr then
      local escaped = text.escapeMagic(next)
      next = make_pattern(escaped)
      if next ~= escaped then
        has_globits = true
      end
    end
    glob_pattern = glob_pattern .. next
  end
  if not has_globits then
    return sh.internal.expand(word)
  end
  local globs = sh.internal.glob(glob_pattern)
  return #globs == 0 and sh.internal.expand(word) or globs
end

function sh.hintHandler(full_line, cursor)
  return sh.internal.hintHandlerImpl(full_line, cursor)
end

function sh.internal.buildCommandRedirects(args)
  local input, output, mode = nil, nil, "write"
  local tokens = args
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
  return args, input, output, mode
end

function sh.internal.parseCommand(words)
  checkArg(1, words, "table")
  if #words == 0 then
    return nil
  end
  local evaluated_words = {}
  for i=1,#words do
    for _, arg in ipairs(sh.internal.evaluate(words[i])) do
      table.insert(evaluated_words, arg)
    end
  end
  local program, reason = shell.resolve(evaluated_words[1], "lua")
  if not program then
    return nil, evaluated_words[1] .. ": " .. reason
  end
  return program, sh.internal.buildCommandRedirects(tx.sub(evaluated_words, 2))
end

function sh.internal.buildPipeStream(commands, env)
  -- Piping data between programs works like so:
  -- program1 gets its output replaced with our custom stream.
  -- program2 gets its input replaced with our custom stream.
  -- repeat for all programs
  -- custom stream triggers execution of "next" program after write.
  -- custom stream triggers yield before read if buffer is empty.
  -- custom stream may have "redirect" entries for fallback/duplication.
  local threads, pipes, inputs, outputs = {}, {}, {}, {}
  for i = 1, #commands do
    local program, args, input, output, mode = table.unpack(commands[i])
    local process_name = tostring(program)
    local reason
    local thread_env = type(program) == "string" and env or nil
    threads[i], reason = process.load(program, thread_env, function()
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
    end, process_name)
    if not threads[i] then
      return false, reason
    end

    if i < #commands then
      pipes[i] = require("buffer").new("rw", sh.internal.newMemoryStream())
      pipes[i]:setvbuf("no")
    end
    if i > 1 then
      pipes[i - 1].stream.next = threads[i]
      pipes[i - 1].stream.args = args
    end
  end
  return threads, pipes, inputs, outputs
end

function sh.internal.executePipeStream(threads, pipes, inputs, outputs, args)
  local result = {}
  for i = 1, #threads do
    -- Emulate CC behavior by making yields a filtered event.pull()
    while args[1] and coroutine.status(threads[i]) ~= "dead" do
      result = table.pack(coroutine.resume(threads[i], table.unpack(args, 2, args.n)))
      if coroutine.status(threads[i]) ~= "dead" then
        local action = result[2]
        if action == nil or type(action) == "number" then
          args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
        else
          args = table.pack(coroutine.yield(table.unpack(result, 2, result.n)))
        end
        -- in case this was the end of the line, args is returned
        result = args
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
  for _, input in ipairs(inputs) do input:close() end
  for _, output in ipairs(outputs) do output:close() end
  return table.unpack(result)
end

function sh.internal.executeStatement(env, commands, eargs)
  local threads, pipes, inputs, outputs = sh.internal.buildPipeStream(commands, env)
  if not threads then return false, pipes end
  local args = tx.concat({true,n=1},commands[1][2] or {}, eargs)
  return sh.internal.executePipeStream(threads, pipes, inputs, outputs, args)
end

function sh.internal.executePipes(pipe_parts, eargs)
  local commands = {}
  for i=1,#pipe_parts do
    commands[i] = table.pack(sh.internal.parseCommand(pipe_parts[i]))
    if commands[i][1] == nil then
      local err = commands[i][2]
      if type(err) == "string" then
        io.stderr:write(err,"\n")
      end
      return sh.internal.ec.parseCommand
    end
  end
  local result = table.pack(sh.internal.executeStatement(env,commands,eargs))
  local cmd_result = result[2]
  if not result[1] then
    if cmd_result then
      if type(cmd_result) == "string" then
        cmd_result = cmd_result:gsub("^/lib/process%.lua:%d+: /", '/')
      end
      io.stderr:write(tostring(cmd_result),"\n")
    end
    return sh.internal.ec.sysError
  end
  return cmd_result
end

function sh.execute(env, command, ...)
  checkArg(2, command, "string")
  local eargs = {...}
  if command:find("^%s*#") then return true, 0 end
  local statements, reason = sh.internal.statements(command)
  if not statements or statements == true then
    return statements, reason
  elseif #statements == 0 then
    return true, 0
  end

  -- simple
  if reason then
    sh.internal.ec.last = sh.internal.command_result_as_code(sh.internal.executePipes(statements,eargs))
    return true
  end

  return sh.internal.execute_complex(statements)
end

function --[[@delayloaded-start@]] sh.internal.glob(glob_pattern)
  local segments = text.split(glob_pattern, {"/"}, true)
  local hiddens = tx.select(segments,function(e)return e:match("^%%%.")==nil end)
  local function is_visible(s,i) 
    return not hiddens[i] or s:match("^%.") == nil 
  end

  local function magical(s)
    for _,glob_rule in ipairs(sh.internal.globbers) do
      if s:match("[^%%]-"..text.escapeMagic(glob_rule[2])) then
        return true
      end
    end
  end

  local is_abs = glob_pattern:sub(1, 1) == "/"
  local root = is_abs and '' or shell.getWorkingDirectory()
  local paths = {is_abs and "/" or ''}
  local relative_separator = ''

  for i,segment in ipairs(segments) do
    local enclosed_pattern = string.format("^(%s)/?$", segment)
    local next_paths = {}
    for _,path in ipairs(paths) do
      if fs.isDirectory(root..path) then
        if magical(segment) then
          for file in fs.list(root..path) do
            if file:match(enclosed_pattern) and is_visible(file, i) then
              table.insert(next_paths, path..relative_separator..file:gsub("/+$",''))
            end
          end
        else -- not a globbing segment, just use it raw
          local plain = text.removeEscapes(segment)
          local fpath = root..path..relative_separator..plain
          local hit = path..relative_separator..plain:gsub("/+$",'')
          if fs.exists(fpath) then
            table.insert(next_paths, hit)
          end
        end
      end
    end
    paths = next_paths
    if not next(paths) then break end
    relative_separator = "/"
  end
  -- if no next_paths were hit here, the ENTIRE glob value is not a path
  return paths
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.getMatchingPrograms(baseName)
  local result = {}
  local result_keys = {} -- cache for fast value lookup
  -- TODO only matching files with .lua extension for now, might want to
  --      extend this to other extensions at some point? env var? file attrs?
  if not baseName or #baseName == 0 then
    baseName = "^(.*)%.lua$"
  else
    baseName = "^(" .. text.escapeMagic(baseName) .. ".*)%.lua$"
  end
  for basePath in string.gmatch(os.getenv("PATH"), "[^:]+") do
    for file in fs.list(basePath) do
      local match = file:match(baseName)
      if match and not result_keys[match] then
        table.insert(result, match)
        result_keys[match] = true
      end
    end
  end
  return result
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.getMatchingFiles(basePath, name)
  local resolvedPath = shell.resolve(basePath)
  local result, baseName = {}

  -- note: we strip the trailing / to make it easier to navigate through
  -- directories using tab completion (since entering the / will then serve
  -- as the intention to go into the currently hinted one).
  -- if we have a directory but no trailing slash there may be alternatives
  -- on the same level, so don't look inside that directory... (cont.)
  if fs.isDirectory(resolvedPath) and name == "" then
    baseName = "^(.-)/?$"
  else
    baseName = "^(" .. text.escapeMagic(name) .. ".-)/?$"
  end

  for file in fs.list(resolvedPath) do
    local match = file:match(baseName)
    if match then
      table.insert(result, basePath ..  match)
    end
  end
  -- (cont.) but if there's only one match and it's a directory, *then* we
  -- do want to add the trailing slash here.
  if #result == 1 and fs.isDirectory(shell.resolve(result[1])) then
    result[1] = result[1] .. "/"
  end
  return result
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.internal.hintHandlerSplit(line)
  if line:sub(-1):find("%s") then
    return '', line
  end
  local splits = text.internal.tokenize(line)
  if not splits then -- parse error, e.g. unclosed quotes
    return nil -- no split, no hints
  end
  local num_splits = #splits
  if num_splits == 1 or not isWordOf(splits[num_splits-1],{";","&&","||","|"}) then
    return '', line
  end
  local l = text.internal.normalize({splits[num_splits]})[1]
  return line:sub(1,-unicode.len(l)-1), l
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.internal.hintHandlerImpl(full_line, cursor)
  local line = unicode.sub(full_line, 1, cursor - 1)
  local suffix = unicode.sub(full_line, cursor)
  if not line or #line < 1 then
    return {}
  end
  local prev,line = sh.internal.hintHandlerSplit(line)
  if not prev then -- failed to parse, e.g. unclosed quote, no hints
    return {}
  end
  local result
  local prefix, partial = line:match("^(.*=)(.*)$")
  if not prefix then prefix, partial = line:match("^(.+%s+)(.*)$") end
  local partialPrefix = (partial or line)
  local name = partialPrefix:gsub(".*/", "")
  partialPrefix = partialPrefix:sub(1, -unicode.len(name) - 1)
  local searchInPath = not prefix and not partialPrefix:find("/")
  if searchInPath then
    result = sh.getMatchingPrograms(line)
  else
    result = sh.getMatchingFiles(partialPrefix, name)
  end
  local resultSuffix = suffix
  if #result > 0 and unicode.sub(result[1], -1) ~= "/" and
     not suffix:sub(1,1):find('%s') and
     (#result == 1 or searchInPath or not prefix) then 
    resultSuffix  = " " .. resultSuffix 
  end
  prefix = prev .. (prefix or "")
  table.sort(result)
  for i = 1, #result do
    result[i] = prefix .. result[i] .. resultSuffix
  end
  return result
end --[[@delayloaded-end@]] 

-- verifies that no pipes are doubled up nor at the start nor end of words
function --[[@delayloaded-start@]] sh.internal.hasValidPiping(words, pipes)
  checkArg(1, words, "table")
  checkArg(2, pipes, "table", "nil")

  if #words == 0 then
    return true
  end

  pipes = pipes or tx.sub(text.syntax, 2) -- first text syntax is ; which CAN be repeated

  local pies = tx.select(words, function(parts, i, t)
    return (#parts == 1 and tx.first(pipes, {{parts[1].txt}}) and true or false), i
  end)

  local bad_pipe
  local last = 0
  for k,v in ipairs(pies) do
    if v then
      if k-last == 1 then
        bad_pipe = words[k][1].txt
        break
      end
      last=k
    end
  end

  if not bad_pipe and last == #pies then
    bad_pipe = words[last][1].txt
  end

  if bad_pipe then
    return false, "parse error near " .. bad_pipe
  else
    return true
  end
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.boolean_executor(chains, predicator)
  local function not_gate(result)
    return sh.internal.command_passed(result) and 1 or 0
  end

  local last = true
  local boolean_stage = 1
  local negation_stage = 2
  local command_stage = 0
  local stage = negation_stage
  local skip = false

  for ci=1,#chains do
    local next = chains[ci]
    local single = #next == 1 and #next[1] == 1 and not next[1][1].qr and next[1][1].txt

    if single == "||" then
      if stage ~= command_stage or #chains == 0 then
        return nil, "syntax error near unexpected token '"..single.."'"
      end
      if sh.internal.command_passed(last) then
        skip = true
      end
      stage = boolean_stage
    elseif single == "&&" then
      if stage ~= command_stage or #chains == 0 then
        return nil, "syntax error near unexpected token '"..single.."'"
      end
      if not sh.internal.command_passed(last) then
        skip = true
      end
      stage = boolean_stage
    elseif not skip then
      local chomped = #next
      local negate = sh.internal.remove_negation(next)
      chomped = chomped ~= #next
      if negate then
        local prev = predicator
        predicator = function(n,i)
          local result = not_gate(prev(n,i))
          predicator = prev
          return result
        end
      end
      if chomped then
        stage = negation_stage
      end
      if #next > 0 then
        last = predicator(next,ci)
        stage = command_stage
      end
    else
      skip = false
      stage = command_stage
    end
  end

  if stage == negation_stage then
    last = not_gate(last)
  end

  return last
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.splitStatements(words, semicolon)
  checkArg(1, words, "table")
  checkArg(2, semicolon, "string", "nil")
  semicolon = semicolon or ";"
  
  return tx.partition(words, function(g, i, t)
    if isWord(g,semicolon) then
      return i, i
    end
  end, true)
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.splitChains(s,pc)
  checkArg(1, s, "table")
  checkArg(2, pc, "string", "nil")
  pc = pc or "|"
  return tx.partition(s, function(w)
    -- each word has multiple parts due to quotes
    if isWord(w,pc) then
      return true
    end
  end, true) -- drop |s
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.groupChains(s)
  checkArg(1,s,"table")
  return tx.partition(s,function(w)return isWordOf(w,{"&&","||"})end)
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.internal.remove_negation(chain)
  if isWord(chain[1],"!") then
    table.remove(chain, 1)
    return true and not sh.internal.remove_negation(chain)
  end
  return false
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.internal.newMemoryStream()
  local memoryStream = {}

  function memoryStream:close()
    self.closed = true
  end

  function memoryStream:seek()
    return nil, "bad file descriptor"
  end

  function memoryStream:read(n)
    if self.closed then
      return nil -- eof
    end
    if self.redirect.read then
      -- popen could be using this code path
      -- if that is the case, it is important to leave stream.buffer alone
      return self.redirect.read:read(n)
    elseif self.buffer == "" then
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
    elseif self.redirect.write then
      return self.redirect.write:write(value)
    elseif not self.closed then
      self.buffer = self.buffer .. value
      self.result = table.pack(coroutine.resume(self.next, table.unpack(self.args)))
      if coroutine.status(self.next) == "dead" then
        self:close()
      end
      if not self.result[1] then
        error(self.result[2], 0)
      end
      table.remove(self.result, 1)
      return self
    end
    return nil, 'stream closed'
  end

  local stream = {closed = false, buffer = "",
                  redirect = {}, result = {}, args = {}}
  local metatable = {__index = memoryStream,
                     __metatable = "memorystream"}
  return setmetatable(stream, metatable)
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.execute_complex(statements)
  for si=1,#statements do local s = statements[si]
    local chains = sh.internal.groupChains(s)
    local last_code,br = sh.internal.boolean_executor(chains, function(chain, chain_index)
      local pipe_parts = sh.internal.splitChains(chain)
      return sh.internal.executePipes(pipe_parts,
        chain_index == #chains and si == #statements and eargs or {})
    end)
    if br then
      io.stderr:write(br,"\n")
    end
    sh.internal.ec.last = sh.internal.command_result_as_code(last_code)
  end
  return true, br
end --[[@delayloaded-end@]]

return sh, local_env
