local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")
local tx = require("transforms")
local unicode = require("unicode")

local sh = {}
sh.internal = {}

-- --[[@@]] are not just comments, but custom annotations for delayload methods.
-- See package.lua and the api wiki for more information
local function isWordOf(w, vs) return w and #w == 1 and not w[1].qr and tx.first(vs,{{w[1].txt}}) ~= nil end
local function isWord(w,v) return isWordOf(w,{v}) end
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
    error("${" .. key .. "}: bad substitution")
  end)
  if expanded:find('`') then
    expanded = sh.internal.parse_sub(expanded)
  end
  return expanded
end

function sh.internal.expand(word)
  if #word == 0 then return {} end
  local result = ''
  for i=1,#word do
    local part = word[i]
    -- sh.expand runs command substitution on backticks
    -- if the entire quoted area is backtick quoted, then
    -- we can save some checks by adding them back in
    local q = part.qr and part.qr[1] == '`' and '`' or ''
    result = result .. (not (part.qr and part.qr[3]) and sh.expand(q..part.txt..q) or part.txt)
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
  local eword = evaluated_words[1]
  local possible_dir_path = shell.resolve(eword)
  if possible_dir_path and fs.isDirectory(possible_dir_path) then
    return nil, string.format("%s: is a directory", eword)
  end
  local program, reason = shell.resolve(eword, "lua")
  if not program then
    return nil, eword .. ": " .. reason
  end
  evaluated_words = tx.sub(evaluated_words, 2)
  return program, evaluated_words
end

function sh.internal.createThreads(commands, eargs, env)
  -- Piping data between programs works like so:
  -- program1 gets its output replaced with our custom stream.
  -- program2 gets its input replaced with our custom stream.
  -- repeat for all programs
  -- custom stream triggers execution of "next" program after write.
  -- custom stream triggers yield before read if buffer is empty.
  -- custom stream may have "redirect" entries for fallback/duplication.
  local threads = {}
  for i = 1, #commands do
    local program, args = table.unpack(commands[i])
    local name, thread = tostring(program)
    local thread_env = type(program) == "string" and env or nil
    local thread, reason = process.load(program, thread_env, function()
      os.setenv("_", name)
      -- popen expects each process to first write an empty string
      -- this is required for proper thread order
      io.write('')
    end, name)

    threads[i] = thread
    
    if thread then
      -- smart check if ios should be loaded
      if tx.first(args, function(token) return token == "<" or token:find(">") end) then
        args, reason = sh.internal.buildCommandRedirects(thread, args)
      end
    end
    
    if not args or not thread then
      for i,t in ipairs(threads) do
        process.internal.close(t)
      end
      return nil, reason
    end

    process.info(thread).data.args = tx.concat(args, eargs or {})
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
    local thread, args = threads[i]
    while coroutine.status(thread) ~= "dead" do
      args = args or process.info(thread).data.args
      result = table.pack(coroutine.resume(thread, table.unpack(args)))
      if coroutine.status(thread) ~= "dead" then
        args = sh.internal.handleThreadYield(result)
        -- in case this was the end of the line, args is returned
        result = args
        if table.remove(args, 1) then
          break
        end
      end
    end
    if not result[1] then
      sh.internal.handleThreadCrash(thread, result)
      break
    end
  end
  return table.unpack(result)
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
  local threads, reason = sh.internal.createThreads(commands, eargs, env)  
  if not threads then
    io.stderr:write(reason,"\n")
    return false
  end
  local result, cmd_result = sh.internal.runThreads(threads)

  if not result then
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
  if command:find("^%s*#") then return true, 0 end
  local statements, reason = sh.internal.statements(command)
  if not statements or statements == true then
    return statements, reason
  elseif #statements == 0 then
    return true, 0
  end

  local eargs = {...}

  -- simple
  if reason then
    sh.internal.ec.last = sh.internal.command_result_as_code(sh.internal.executePipes(statements, eargs, env))
    return true
  end

  return sh.internal.execute_complex(statements, eargs, env)
end

function --[[@delayloaded-start@]] sh.internal.handleThreadYield(result)
  local action = result[2]
  if action == nil or type(action) == "number" then
    return table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
  else
    return table.pack(coroutine.yield(table.unpack(result, 2, result.n)))
  end
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.handleThreadCrash(thread, result)
  if type(result[2]) == "table" and result[2].reason == "terminated" then
    if result[2].code then
      result[1] = true
      result.n = 1
    else
      result[2] = "terminated"
    end
  elseif type(result[2]) == "string" then
    result[2] = debug.traceback(thread, result[2])
  end
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.buildCommandRedirects(thread, args)
  local data = process.info(thread).data
  local tokens, ios, handles = args, data.io, data.handles
  args = {}
  local from_io, to_io, mode
  for i = 1, #tokens do
    local token = tokens[i]
    if token == "<" then
      from_io = 0
      mode = "r"
    else
      local first_index, last_index, from_io_txt, mode_txt, to_io_txt = token:find("(%d*)(>>?)(.*)")
      if mode_txt then
        mode = mode_txt == ">>" and "a" or "w"
        from_io = from_io_txt and tonumber(from_io_txt) or 1
        if to_io_txt ~= "" then
          to_io = tonumber(to_io_txt:sub(2))
          ios[from_io] = ios[to_io]
          mode = nil
        end
      else -- just an arg
        if not mode then
          table.insert(args, token)
        else
          local file, reason = io.open(shell.resolve(token), mode)
          if not file then
            return nil, "could not open '" .. token .. "': " .. reason
          end
          table.insert(handles, file)
          ios[from_io] = file
        end
        mode = nil
      end
    end
  end

  return args
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.buildPipeChain(threads)
  local prev_pipe
  for i=1,#threads do
    local thread = threads[i]
    local data = process.info(thread).data
    local pio = data.io

    local pipe
    if i < #threads then
      pipe = require("buffer").new("rw", sh.internal.newMemoryStream())
      pipe:setvbuf("no", 0)
      -- buffer close flushes the buffer, but we have no buffer
      -- also, when the buffer is closed, read and writes don't pass through
      -- simply put, we don't want buffer:close
      pipe.close = function(self) self.stream:close() end
      pipe.stream.redirect[1] = rawget(pio, 1)
      pio[1] = pipe
      table.insert(data.handles, pipe)
    end

    if prev_pipe then
      prev_pipe.stream.redirect[0] = rawget(pio, 0)
      prev_pipe.stream.next = thread
      pio[0] = prev_pipe
    end

    prev_pipe = pipe
  end

end --[[@delayloaded-end@]]

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
  local root = is_abs and '' or shell.getWorkingDirectory():gsub("([^/])$","%1/")
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
    for file in fs.list(shell.resolve(basePath)) do
      local match = file:match(baseName)
      if match and not result_keys[match] then
        table.insert(result, match)
        result_keys[match] = true
      end
    end
  end
  return result
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.getMatchingFiles(partial_path)
  -- name: text of the partial file name being expanded
  local name = partial_path:gsub("^.*/", "")
  -- here we remove the name text from the partialPrefix
  local basePath = unicode.sub(partial_path, 1, -unicode.len(name) - 1)

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
      table.insert(result, basePath ..  match:gsub("(%s)", "\\%1"))
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
  -- I do not plan on having text tokenizer parse error on
  -- trailiing \ in case of future support for multiple line
  -- input. But, there are also no hints for it
  if line:match("\\$") then return nil end

  local splits, simple = text.internal.tokenize(line,{show_escapes=true})
  if not splits then -- parse error, e.g. unclosed quotes
    return nil -- no split, no hints
  end

  local num_splits = #splits

  -- search for last statement delimiters
  local last_close = 0
  for index = num_splits, 1, -1 do
    local word = splits[index]
    if isWordOf(word, {";","&&","||","|"}) then
      last_close = index
      break
    end
  end

  -- if the very last word of the line is a delimiter
  -- consider this a fresh new, empty line
  -- this captures edge cases with empty input as well (i.e. no splits)
  if last_close == num_splits then
    return nil -- no hints on empty command
  end

  local last_word = splits[num_splits]
  local normal = text.internal.normalize({last_word})[1]

  -- if there is white space following the words
  -- and we have at least one word following the last delimiter
  -- then in all cases we are looking for ANY arg
  if unicode.sub(line, -unicode.len(normal)) ~= normal then
    return line, nil, ""
  end

  local prefix = unicode.sub(line, 1, -unicode.len(normal) - 1)

  -- renormlizing the string will create 'printed' quality text
  normal = text.internal.normalize(text.internal.tokenize(normal), true)[1]

  -- one word: cmd
  -- many: arg
  if last_close == num_splits - 1 then
    return prefix, normal, nil
  else
    return prefix, nil, normal
  end
end --[[@delayloaded-end@]] 

function --[[@delayloaded-start@]] sh.internal.hintHandlerImpl(full_line, cursor)
  -- line: text preceding the cursor: we want to hint this part (expand it)
  local line = unicode.sub(full_line, 1, cursor - 1)
  -- suffix: text following the cursor (if any, else empty string) to append to the hints
  local suffix = unicode.sub(full_line, cursor)

  -- hintHandlerSplit helps make the hints work even after delimiters such as ;
  -- it also catches parse errors such as unclosed quotes
  -- prev: not needed for this hint
  -- cmd: the command needing hint
  -- arg: the argument needing hint
  local prev, cmd, arg = sh.internal.hintHandlerSplit(line)

  -- also, if there is no text to hint, there are no hints
  if not prev then -- no hints e.g. unclosed quote, e.g. no text
    return {}
  end
  local result

  local searchInPath = cmd and not cmd:find("/")
  if searchInPath then
    result = sh.getMatchingPrograms(cmd)
  else
    -- special arg issue, after equal sign
    if arg then
      local equal_index = arg:find("=[^=]*$")
      if equal_index then
        prev = prev .. unicode.sub(arg, 1, equal_index)
        arg = unicode.sub(arg, equal_index + 1)
      end
    end
    result = sh.getMatchingFiles(cmd or arg)
  end

  -- in very special cases, the suffix should include a blank space to indicate to the user that the hint is discrete
  local resultSuffix = suffix
  if #result > 0 and unicode.sub(result[1], -1) ~= "/" and
     not suffix:sub(1,1):find('%s') and
     #result == 1 or searchInPath then 
    resultSuffix  = " " .. resultSuffix 
  end

  table.sort(result)
  for i = 1, #result do
    -- the hints define the whole line of text
    result[i] = prev .. result[i] .. resultSuffix
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

  local semi_split = tx.find(text.syntax, {";"}) -- all symbols before ; in syntax CAN be repeated
  pipes = pipes or tx.sub(text.syntax, semi_split + 1)

  local pies = tx.select(words, function(parts, i)
    return #parts == 1 and #text.split(parts[1].txt, pipes, true) == 0 and true or false
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
    self.redirect = {}
  end

  function memoryStream:seek()
    return nil, "bad file descriptor"
  end

  function memoryStream:read(n)
    if self.closed then
      return nil -- eof
    end
    if self.redirect[0] then
      -- popen could be using this code path
      -- if that is the case, it is important to leave stream.buffer alone
      return self.redirect[0]:read(n)
    elseif self.buffer == "" then
      process.info(self.next).data.args = table.pack(coroutine.yield(table.unpack(self.result)))
    end
    local result = string.sub(self.buffer, 1, n)
    self.buffer = string.sub(self.buffer, n + 1)
    return result
  end

  function memoryStream:write(value)
    if not self.redirect[1] and self.closed then
      -- if next is dead, ignore all writes
      if coroutine.status(self.next) ~= "dead" then
        error("attempt to use a closed stream")
      end
    elseif self.redirect[1] then
      return self.redirect[1]:write(value)
    elseif not self.closed then
      self.buffer = self.buffer .. value
      local args = process.info(self.next).data.args
      self.result = table.pack(coroutine.resume(self.next, table.unpack(args)))
      if coroutine.status(self.next) == "dead" then
        self:close()
      end
      if not self.result[1] then
        error(self.result[2], 0)
      end
      table.remove(self.result, 1)
      return self
    end
    os.exit(0) -- abort the current process: SIGPIPE
  end

  local stream = {closed = false, buffer = "",
                  redirect = {}, result = {}}
  local metatable = {__index = memoryStream,
                     __metatable = "memorystream"}
  return setmetatable(stream, metatable)
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] sh.internal.execute_complex(statements, eargs, env)
  for si=1,#statements do local s = statements[si]
    local chains = sh.internal.groupChains(s)
    local last_code = sh.internal.boolean_executor(chains, function(chain, chain_index)
      local pipe_parts = sh.internal.splitChains(chain)
      local next_args = chain_index == #chains and si == #statements and eargs or {}
      return sh.internal.executePipes(pipe_parts, next_args, env)
    end)
    sh.internal.ec.last = sh.internal.command_result_as_code(last_code)
  end
  return true
end --[[@delayloaded-end@]]


function --[[@delayloaded-start@]] sh.internal.parse_sub(input)
  -- cannot use gsub here becuase it is a [C] call, and io.popen needs to yield at times
  local packed = {}
  -- not using for i... because i can skip ahead
  local i, len = 1, #input

  while i < len do

    local fi, si, capture = input:find("`([^`]*)`", i)

    if not fi then
      table.insert(packed, input:sub(i))
      break
    end

    local sub = io.popen(capture)
    local result = sub:read("*a")
    sub:close()
    -- all whitespace is replaced by single spaces
    -- we requote the result because tokenize will respect this as text
    table.insert(packed, (text.trim(result):gsub("%s+"," ")))

    i = si+1
  end

  return table.concat(packed)
end --[[@delayloaded-end@]]

return sh, local_env
