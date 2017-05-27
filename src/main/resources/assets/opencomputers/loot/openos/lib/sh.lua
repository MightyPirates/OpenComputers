local process = require("process")
local shell = require("shell")
local text = require("text")
local tx = require("transforms")

local sh = {}
sh.internal = {}

function sh.internal.isWordOf(w, vs)
 return w and #w == 1 and not w[1].qr and tx.first(vs,{{w[1].txt}}) ~= nil
end

local isWordOf = sh.internal.isWordOf

-------------------------------------------------------------------------------

--SH API

sh.internal.globbers = {{"*",".*"},{"?","."}}
sh.internal.ec = {}
sh.internal.ec.parseCommand = 127
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
  elseif ec == nil or ec == true then
    return 0
  elseif type(ec) ~= "number" then
    return 2 -- illegal number
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

-- expand (interpret) a single quoted area
-- examples: $foo, "$foo", or `cmd` in back ticks
function sh.expand(value)
  local expanded = value
  :gsub("%$([_%w%?]+)", function(key)
    if key == "?" then
      return tostring(sh.getLastExitCode())
    end
    return os.getenv(key) or ''
  end)
  :gsub("%${(.*)}", function(key)
    if sh.internal.isIdentifier(key) then
      return sh.internal.expandKey(key)
    end
    io.stderr:write("${" .. key .. "}: bad substitution\n")
    os.exit(1)
  end)
  return expanded
end

-- expand all parts if interpreted (not literal)
-- i.e '' is literal, "" and `` are interpreted
-- called only by sh.internal.evaluate
function sh.internal.expand(word)
  if #word == 0 then return {} end
  local result = ''
  for i=1,#word do
    local part = word[i]
    local next = part.txt
    local quoted = part.qr
    local literal, keep_whitespace, sub
    if quoted then
      literal = quoted[3]
      keep_whitespace = quoted[1] == '"'
      sub = quoted[1]:match('`') or next:find('`') and ''
    end
    if not literal then
      next = sh.expand(next)
      if sub then
        next = sh.internal.parse_sub(sub .. next .. sub)
      end
      if not keep_whitespace then
        next = text.trim((next:gsub("%s+", " ")))
      end
    end
    result = result .. next
  end
  return {result}
end

-- expand to files in path, or try key substitution
-- word is a list of metadata-filled word parts
-- note: text.internal.words(string) returns an array of these words
function sh.internal.evaluate(word)
  checkArg(1, word, "table")
  local glob_pattern = ''
  local has_globits = false
  for i=1,#word do
    local part = word[i]
    local next = part.txt
    if not part.qr then
      local escaped = text.escapeMagic(next)
      next = escaped
      for _,glob_rule in ipairs(sh.internal.globbers) do
        next = next:gsub("%%%"..glob_rule[1], glob_rule[2])
        while true do
          local prev = next
          next = next:gsub(text.escapeMagic(glob_rule[2]):rep(2), glob_rule[2])
          if prev == next then
            break
          end
        end
      end
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

function sh.internal.parseCommand(words)
  checkArg(1, words, "table")
  if #words == 0 then
    return nil
  end
  -- evaluated words
  local ewords = {}
  -- the arguments have < or > which require parsing for redirection
  local has_tokens
  for i=1,#words do
    for _, arg in ipairs(sh.internal.evaluate(words[i])) do
      table.insert(ewords, arg)
      has_tokens = has_tokens or arg:find("[<>]")
    end
  end
  return table.remove(ewords, 1), ewords, has_tokens
end

function sh.internal.createThreads(commands, env, start_args)
  -- Piping data between programs works like so:
  -- program1 gets its output replaced with our custom stream.
  -- program2 gets its input replaced with our custom stream.
  -- repeat for all programs
  -- custom stream triggers execution of "next" program after write.
  -- custom stream triggers yield before read if buffer is empty.
  -- custom stream may have "redirect" entries for fallback/duplication.
  local threads = {}
  for i = 1, #commands do
    local program, c_args, c_has_tokens = table.unpack(commands[i])
    local name = tostring(program)
    local thread_env = type(program) == "string" and env or nil
    local thread, reason = process.load(program, thread_env, function(...)
      local cdata = process.info().data.command
      local args, has_tokens, start_args = cdata.args, cdata.has_tokens, cdata.start_args
      if has_tokens then
        args = sh.internal.buildCommandRedirects(args)
      end

      sh.internal.concatn(args, start_args)
      sh.internal.concatn(args, {...}, select('#', ...))

      -- popen expects each process to first write an empty string
      -- this is required for proper thread order
      io.write("")
      return table.unpack(args, 1, args.n or #args)
    end, name)

    threads[i] = thread

    if not thread then
      for i,t in ipairs(threads) do
        process.internal.close(t)
      end
      return nil, reason
    end

    local pdata = process.info(thread).data
    pdata.command =
    {
      args = c_args,
      has_tokens = c_has_tokens,
      start_args = start_args and start_args[i] or {}
    }

  end

  if #threads > 1 then
    sh.internal.buildPipeChain(threads)
  end

  return threads
end

function sh.internal.runThreads(threads)
  local result = {}
  for i = 1, #threads do
    -- Emulate CC behavior by making yields a filtered event.pull()
    local thread, args = threads[i], {}
    while coroutine.status(thread) ~= "dead" do
      result = table.pack(coroutine.resume(thread, table.unpack(args)))
      if coroutine.status(thread) ~= "dead" then
        args = table.pack(coroutine.yield(table.unpack(result, 2, result.n)))
      elseif not result[1] then
        io.stderr:write(result[2])
      end
    end
  end
  return result[2]
end

function sh.internal.executePipes(pipe_parts, eargs, env)
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
  local threads, reason = sh.internal.createThreads(commands, env, {[#commands]=eargs})  
  if not threads then
    io.stderr:write(reason,"\n")
    return false
  end
  return sh.internal.runThreads(threads)
end

function sh.execute(env, command, ...)
  checkArg(2, command, "string")
  if command:find("^%s*#") then return true, 0 end
  local statements, reason = sh.internal.statements(command)
  if not statements or statements == true then
    return statements, reason
  elseif #statements == 0 then
    return true, 0
  end

  -- MUST be table.pack for non contiguous ...
  local eargs = table.pack(...)

  -- simple
  if reason then
    sh.internal.ec.last = sh.internal.command_result_as_code(sh.internal.executePipes(statements, eargs, env))
    return true
  end

  return sh.internal.execute_complex(statements, eargs, env)
end

function sh.internal.concatn(apack, bpack, bn)
  local an = (apack.n or #apack)
  bn = bn or bpack.n or #bpack
  for i=1,bn do
    apack[an + i] = bpack[i]
  end
  apack.n = an + bn
end

setmetatable(sh,
{
  __index = function(tbl, key)
    setmetatable(sh.internal, nil)
    setmetatable(sh, nil)
    dofile("/opt/core/full_sh.lua")
    return rawget(tbl, key)
  end
})

setmetatable(sh.internal, getmetatable(sh))

return sh
