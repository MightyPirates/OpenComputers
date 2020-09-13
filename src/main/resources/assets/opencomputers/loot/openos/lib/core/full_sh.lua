local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local text = require("text")
local tx = require("transforms")
local unicode = require("unicode")

local sh = require("sh")

local isWordOf = sh.internal.isWordOf

-------------------------------------------------------------------------------

function sh.internal.command_passed(ec)
  return sh.internal.command_result_as_code(ec) == 0
end

-- takes ewords and searches for redirections (may not have any)
-- removes the redirects and their arguments from the ewords
-- returns a redirection table that is used during process load
-- returns false if no redirections are defined
-- to open the redirect handles, see openCommandRedirects
function sh.internal.buildCommandRedirects(words)
  local redirects = {}
  local index = 1 -- we move index manually to allow removals from ewords
  local from_io, to_io, mode
  local syn_err_msg = "syntax error near unexpected token "

  -- hasValidPiping has been modified, it does not verify redirects now
  -- we could have bad redirects such as "echo hi > > foo"
  -- we must validate the input here

  while true do
    local word = words[index]
    if not word then break end

    -- redirections are
    -- 1. single part
    -- 2. not quoted
    local part = word[1]
    local token = not word[2] and not part.qr and part.txt or ""
    local _, _, from_io_txt, mode_txt, to_io_txt = token:find("(%d*)([<>]>?)%&?(.*)")
    if mode_txt then
      if mode then
        return nil, syn_err_msg .. token
      end
      mode = assert(({["<"]="r",[">"]="w",[">>"]="a",})[mode_txt], "redirect failed to detect mode")
      from_io = from_io_txt ~= "" and tonumber(from_io_txt) or mode == "r" and 0 or 1
      to_io = to_io_txt ~= "" and tonumber(to_io_txt)
    elseif mode then
      token = sh.internal.evaluate({word})
      if #token > 1 then
        return nil, string.format("%s: ambiguous redirect", part.txt)
      end
      to_io = token[1]
    else
      index = index + 1
    end

    if mode then
      table.remove(words, index)
    end

    if to_io then
      table.insert(redirects, {from_io, to_io, mode})
      mode = nil
      to_io = nil
    end
  end

  if mode then
    return nil, syn_err_msg .. "newline"
  end

  return redirects
end

-- redirects as built by buildCommentRedirects
function sh.internal.openCommandRedirects(redirects)
  local data = process.info().data
  local ios = data.io

  for _,rjob in ipairs(redirects) do
    local from_io, to_io, mode = table.unpack(rjob)

    if type(to_io) == "number" then -- io to io
      -- from_io and to_io should be numbers
      ios[from_io] = io.dup(ios[to_io])
    else
      -- to_io should be a string
      local file, reason = io.open(shell.resolve(to_io), mode)
      if not file then
        io.stderr:write("could not open '" .. to_io .. "': " .. reason .. "\n")
        os.exit(1)
      end
      ios[from_io] = file
    end
  end
end

-- takes an eword, returns a list of glob hits or {word} if no globs exist
function sh.internal.glob(eword)
  -- words are parts, parts are txt and qr
  -- eword.txt is a convenience field of the parts
  -- turn word into regex based on globits
  local globbers = {{"*",".*"},{"?","."}}
  local glob_pattern = ""
  local has_globits
  for _,part in ipairs(eword) do
    local next = part.txt
    -- globs only exist outside quotes
    if not part.qr then
      local escaped = text.escapeMagic(next)
      next = escaped

      for _,glob_rule in ipairs(globbers) do
        --remove duplicates
        while true do
          local prev = next
          next = next:gsub(text.escapeMagic(glob_rule[1]):rep(2), glob_rule[1])
          if prev == next then
            break
          end
        end
        --revert globit
        next = next:gsub("%%%"..glob_rule[1], glob_rule[2])
      end

      -- if next is still equal to escaped that means no globits were detected in this word part
      -- this word may not contain a globit, the prior search did a cheap search for globits
      has_globits = has_globits or next ~= escaped
    end
    glob_pattern = glob_pattern .. next
  end

  if not has_globits then
    return {eword.txt}
  end

  local segments = text.split(glob_pattern, {"/"}, true)
  local hiddens = tx.foreach(segments,function(e)return e:match("^%%%.")==nil end)
  local function is_visible(s,i) 
    return not hiddens[i] or s:match("^%.") == nil 
  end

  local function magical(s)
    for _,glob_rule in ipairs(globbers) do
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
    if not next(paths) then
    -- if no next_paths were hit here, the ENTIRE glob value is not a path
      return {eword.txt}
    end
    relative_separator = "/"
  end
  return paths
end 

function sh.getMatchingPrograms(baseName)
  if not baseName or baseName == "" then return {} end
  local result = {}
  local result_keys = {} -- cache for fast value lookup
  local function check(key)
    if key:find(baseName, 1, true) == 1 and not result_keys[key] then
      table.insert(result, key)
      result_keys[key] = true
    end
  end
  for alias in shell.aliases() do
    check(alias)
  end
  for basePath in string.gmatch(os.getenv("PATH"), "[^:]+") do
    for file in fs.list(shell.resolve(basePath)) do
      check(file:gsub("%.lua$", ""))
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

  local splits = text.internal.tokenize(line,{show_escapes=true})
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

  local semi_split = tx.first(text.syntax, {{";"}}) -- symbols before ; are redirects and follow slightly different rules, see buildCommandRedirects
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
    return false, "syntax error near unexpected token " .. state
  else
    return true
  end
end

function sh.internal.boolean_executor(chains, predicator)
  local function not_gate(result, reason)
    return sh.internal.command_passed(result) and 1 or 0, reason
  end

  local last = true
  local last_reason
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
          local result, reason = not_gate(prev(n,i))
          predicator = prev
          return result, reason
        end
      end
      if chomped then
        stage = negation_stage
      end
      if #next > 0 then
        last, last_reason = predicator(next,ci)
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

  return last, last_reason
end

function sh.internal.splitStatements(words, semicolon)
  checkArg(1, words, "table")
  checkArg(2, semicolon, "string", "nil")
  semicolon = semicolon or ";"
  
  return tx.partition(words, function(g, i)
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
    return not sh.internal.remove_negation(chain)
  end
  return false
end

function sh.internal.execute_complex(words, eargs, env)
  -- we shall validate pipes before any statement execution
  local statements = sh.internal.splitStatements(words)
  for i=1,#statements do
    local ok, why = sh.internal.hasValidPiping(statements[i])
    if not ok then return nil,why end
  end

  for si=1,#statements do local s = statements[si]
    local chains = sh.internal.groupChains(s)
    local last_code, reason = sh.internal.boolean_executor(chains, function(chain, chain_index)
      local pipe_parts = sh.internal.splitChains(chain)
      local next_args = chain_index == #chains and si == #statements and eargs or {}
      return sh.internal.executePipes(pipe_parts, next_args, env)
    end)
    sh.internal.ec.last = sh.internal.command_result_as_code(last_code, reason)
  end
  return sh.internal.ec.last == 0
end

-- params: words[tokenized word list]
-- return: command args, redirects
function sh.internal.evaluate(words)
  local redirects, why = sh.internal.buildCommandRedirects(words)
  if not redirects then
    return nil, why
  end

  do
    local normalized = text.internal.normalize(words)
    local command_text = table.concat(normalized, " ")
    local subbed = sh.internal.parse_sub(command_text)
    if subbed ~= command_text then
      words = text.internal.tokenize(subbed)
    end
  end

  local repack = false
  for _, word in ipairs(words) do
    for _, part in pairs(word) do
      if not (part.qr or {})[3] then
        local expanded = sh.expand(part.txt)
        if expanded ~= part.txt then
          part.txt = expanded
          repack = true
        end
      end
    end
  end

  if repack then
    local normalized = text.internal.normalize(words)
    local command_text = table.concat(normalized, " ")
    words = text.internal.tokenize(command_text)
  end

  local args = {}
  for _, word in ipairs(words) do
    local eword = { txt = "" }
    for _, part in ipairs(word) do
      eword.txt = eword.txt .. part.txt
      eword[#eword + 1] = { qr = part.qr, txt = part.txt }
    end
    for _, arg in ipairs(sh.internal.glob(eword)) do
      args[#args + 1] = arg
    end
  end

  return args, redirects
end

function sh.internal.parse_sub(input, quotes)
  -- unquoted command substituted text is parsed as individual parameters
  -- there is not a concept of "keeping whitespace" as previously thought
  -- we see removal of whitespace only because they are separate arguments
  -- e.g. /echo `echo a    b`/ becomes /echo a b/ quite literally, and the a and b are separate inputs
  -- e.g. /echo a"`echo b c`"d/ becomes /echo a"b c"d/ which is a single input

  if quotes and quotes[1] == '`' then
    input = string.format("`%s`", input)
    quotes[1], quotes[2] = "", "" -- substitution removes the quotes
  end

  -- cannot use gsub here becuase it is a [C] call, and io.popen needs to yield at times
  local packed = {}
  -- not using for i... because i can skip ahead
  local i, len = 1, #input

  while i <= len do
    local fi, si, capture = input:find("`([^`]*)`", i)

    if not fi then
      table.insert(packed, input:sub(i))
      break
    end
    table.insert(packed, input:sub(i, fi - 1))

    local sub = io.popen(capture)
    local result = sub:read("*a")
    sub:close()

    -- command substitution cuts trailing newlines
    table.insert(packed, (result:gsub("\n+$","")))
    i = si+1
  end

  return table.concat(packed)
end


