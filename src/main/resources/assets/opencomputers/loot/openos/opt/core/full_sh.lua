local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local text = require("text")
local tx = require("transforms")
local unicode = require("unicode")

local sh = require("sh")

local isWordOf = sh.internal.isWordOf

-------------------------------------------------------------------------------

function sh.internal.buildCommandRedirects(args, thread)
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
            io.stderr:write("could not open '" .. token .. "': " .. reason .. "\n")
            os.exit(1)
          end
          table.insert(handles, file)
          ios[from_io] = file
        end
        mode = nil
      end
    end
  end

  return args
end

function sh.internal.buildPipeChain(threads)
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
end

function sh.internal.glob(glob_pattern)
  local segments = text.split(glob_pattern, {"/"}, true)
  local hiddens = tx.foreach(segments,function(e)return e:match("^%%%.")==nil end)
  local function is_visible(s,i) 
    return not hiddens[i] or s:match("^%.") == nil 
  end

  local function magical(s)
    for _,glob_rule in ipairs(sh.internal.globbers) do
      if (" "..s):match("[^%%]"..text.escapeMagic(glob_rule[2])) then
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
end 

function sh.getMatchingPrograms(baseName)
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
end 

function sh.getMatchingFiles(partial_path)
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
end 

function sh.internal.hintHandlerSplit(line)
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
end 

function sh.internal.hintHandlerImpl(full_line, cursor)
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
end 

-- verifies that no pipes are doubled up nor at the start nor end of words
function sh.internal.hasValidPiping(words, pipes)
  checkArg(1, words, "table")
  checkArg(2, pipes, "table", "nil")

  if #words == 0 then
    return true
  end

  local semi_split = tx.find(text.syntax, {";"}) -- all symbols before ; in syntax CAN be repeated
  pipes = pipes or tx.sub(text.syntax, semi_split + 1)

  local state = "" -- cannot start on a pipe
  
  for w=1,#words do
    local word = words[w]
    for p=1,#word do
      local part = word[p]
      if part.qr then
        state = nil
      elseif part.txt == "" then
        state = nil -- not sure how this is possible (empty part without quotes?)
      elseif #text.split(part.txt, pipes, true) == 0 then
        local prev = state
        state = part.txt
        if prev then -- cannot have two pipes in a row
          word = nil
          break
        end
      else
        state = nil
      end
    end
    if not word then -- bad pipe
      break
    end
  end

  if state then
    return false, "parse error near " .. state
  else
    return true
  end
end

function sh.internal.boolean_executor(chains, predicator)
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
end

function sh.internal.splitStatements(words, semicolon)
  checkArg(1, words, "table")
  checkArg(2, semicolon, "string", "nil")
  semicolon = semicolon or ";"
  
  return tx.partition(words, function(g, i, t)
    if isWordOf(g, {semicolon}) then
      return i, i
    end
  end, true)
end

function sh.internal.splitChains(s,pc)
  checkArg(1, s, "table")
  checkArg(2, pc, "string", "nil")
  pc = pc or "|"
  return tx.partition(s, function(w)
    -- each word has multiple parts due to quotes
    if isWordOf(w, {pc}) then
      return true
    end
  end, true) -- drop |s
end

function sh.internal.groupChains(s)
  checkArg(1,s,"table")
  return tx.partition(s,function(w)return isWordOf(w,{"&&","||"})end)
end 

function sh.internal.remove_negation(chain)
  if isWordOf(chain[1], {"!"}) then
    table.remove(chain, 1)
    return true and not sh.internal.remove_negation(chain)
  end
  return false
end 

function sh.internal.newMemoryStream()
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
      coroutine.yield()
    end
    local result = string.sub(self.buffer, 1, n)
    self.buffer = string.sub(self.buffer, n + 1)
    return result
  end

  function memoryStream:write(value)
    if not self.redirect[1] and self.closed then
      -- if next is dead, ignore all writes
      if coroutine.status(self.next) ~= "dead" then
        io.stderr:write("attempt to use a closed stream\n")
        os.exit(1)
      end
    elseif self.redirect[1] then
      return self.redirect[1]:write(value)
    elseif not self.closed then
      self.buffer = self.buffer .. value
      local result = table.pack(coroutine.resume(self.next))
      if coroutine.status(self.next) == "dead" then
        self:close()
      end
      if not result[1] then
        io.stderr:write(tostring(result[2]) .. "\n")  
        os.exit(1)
      end
      return self
    end
    os.exit(0) -- abort the current process: SIGPIPE
  end

  local stream = {closed = false, buffer = "",
                  redirect = {}, result = {}}
  local metatable = {__index = memoryStream,
                     __metatable = "memorystream"}
  return setmetatable(stream, metatable)
end

function sh.internal.execute_complex(statements, eargs, env)
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
end


function sh.internal.parse_sub(input)
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
    local result = input:sub(i, fi - 1) .. sub:read("*a")
    sub:close()

    -- command substitution cuts trailing newlines
    table.insert(packed, (result:gsub("\n+$","")))
    i = si+1
  end

  return table.concat(packed)
end


