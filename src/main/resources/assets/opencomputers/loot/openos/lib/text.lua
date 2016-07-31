local unicode = require("unicode")
local tx = require("transforms")

-- --[[@@]] are not just comments, but custom annotations for delayload methods.
-- See package.lua and the api wiki for more information

local text = {}
local local_env = {tx=tx,unicode=unicode}

text.internal = {}

text.syntax = {"^%d?>>?&%d+$",";","&&","||?","^%d?>>?",">>?","<"}

function --[[@delayloaded-start@]] text.detab(value, tabWidth)
  checkArg(1, value, "string")
  checkArg(2, tabWidth, "number", "nil")
  tabWidth = tabWidth or 8
  local function rep(match)
    local spaces = tabWidth - match:len() % tabWidth
    return match .. string.rep(" ", spaces)
  end
  local result = value:gsub("([^\n]-)\t", rep) -- truncate results
  return result
end --[[@delayloaded-end@]]

-- used in motd
function text.padRight(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  if not value or unicode.wlen(value) == 0 then
    return string.rep(" ", length)
  else
    return value .. string.rep(" ", length - unicode.wlen(value))
  end
end

function --[[@delayloaded-start@]] text.padLeft(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  if not value or unicode.wlen(value) == 0 then
    return string.rep(" ", length)
  else
    return string.rep(" ", length - unicode.wlen(value)) .. value
  end
end --[[@delayloaded-end@]]

function text.trim(value) -- from http://lua-users.org/wiki/StringTrim
  local from = string.match(value, "^%s*()")
  return from > #value and "" or string.match(value, ".*%S", from)
end

function text.wrap(value, width, maxWidth)
  checkArg(1, value, "string")
  checkArg(2, width, "number")
  checkArg(3, maxWidth, "number")
  local line, nl = value:match("([^\r\n]*)(\r?\n?)") -- read until newline
  if unicode.wlen(line) > width then -- do we even need to wrap?
    local partial = unicode.wtrunc(line, width)
    local wrapped = partial:match("(.*[^a-zA-Z0-9._()'`=])")
    if wrapped or unicode.wlen(line) > maxWidth then
      partial = wrapped or partial
      return partial, unicode.sub(value, unicode.len(partial) + 1), true
    else
      return "", value, true -- write in new line.
    end
  end
  local start = unicode.len(line) + unicode.len(nl) + 1
  return line, start <= unicode.len(value) and unicode.sub(value, start) or nil, unicode.len(nl) > 0
end

function text.wrappedLines(value, width, maxWidth)
  local line, nl
  return function()
    if value then
      line, value, nl = text.wrap(value, width, maxWidth)
      return line
    end
  end
end

-------------------------------------------------------------------------------

-- separate string value into an array of words delimited by whitespace
-- groups by quotes
-- options is a table used for internal undocumented purposes
function text.tokenize(value, options)
  checkArg(1, value, "string")
  checkArg(2, options, "table", "nil")
  options = options or {}

  local tokens, reason = text.internal.tokenize(value, options)

  if type(tokens) ~= "table" then
    return nil, reason
  end

  if options.doNotNormalize then
    return tokens
  end

  return text.internal.normalize(tokens)
end

function text.escapeMagic(txt)
  return txt:gsub('[%(%)%.%%%+%-%*%?%[%^%$]', '%%%1')
end

function text.removeEscapes(txt)
  return txt:gsub("%%([%(%)%.%%%+%-%*%?%[%^%$])","%1")
end

-------------------------------------------------------------------------------
-- like tokenize, but does not drop any text such as whitespace
-- splits input into an array for sub strings delimited by delimiters
-- delimiters are included in the result if not dropDelims
function --[[@delayloaded-start@]] text.split(input, delimiters, dropDelims, di)
  checkArg(1, input, "string")
  checkArg(2, delimiters, "table")
  checkArg(3, dropDelims, "boolean", "nil")
  checkArg(4, di, "number", "nil")

  if #input == 0 then return {} end
  di = di or 1
  local result = {input}
  if di > #delimiters then return result end

  local function add(part, index, r, s, e)
    local sub = part:sub(s,e)
    if #sub == 0 then return index end
    local subs = r and text.split(sub,delimiters,dropDelims,r) or {sub}
    for i=1,#subs do
      table.insert(result, index+i-1, subs[i])
    end
    return index+#subs
  end

  local i,d=1,delimiters[di]
  while true do
    local next = table.remove(result,i)
    if not next then break end
    local si,ei = next:find(d)
    if si and ei and ei~=0 then -- delim found
      i=add(next, i, di+1, 1, si-1)
      i=dropDelims and i or add(next, i, false, si, ei)
      i=add(next, i, di, ei+1)
    else
      i=add(next, i, di+1, 1, #next)
    end
  end
  
  return result
end --[[@delayloaded-end@]]

-----------------------------------------------------------------------------

function text.internal.tokenize(value, options)
  checkArg(1, value, "string")
  checkArg(2, options, "table", "nil")
  options = options or {}
  local delimiters = options.delimiters
  local custom = not not options.delimiters
  delimiters = delimiters or text.syntax

  local words, reason = text.internal.words(value, options)

  local splitter = text.escapeMagic(custom and table.concat(delimiters) or "<>|;&")
  if type(words) ~= "table" or 
    #splitter == 0 or
    not value:find("["..splitter.."]") then
    return words, reason
  end

  return text.internal.splitWords(words, delimiters)
end

-- tokenize input by quotes and whitespace
function text.internal.words(input, options)
  checkArg(1, input, "string")
  checkArg(2, options, "table", "nil")
  options = options or {}
  local quotes = options.quotes
  local show_escapes = options.show_escapes
  local qr = nil
  quotes = quotes or {{"'","'",true},{'"','"'},{'`','`'}}
  local function append(dst, txt, qr)
    local size = #dst
    if size == 0 or dst[size].qr ~= qr then
      dst[size+1] = {txt=txt, qr=qr}
    else
      dst[size].txt = dst[size].txt..txt
    end
  end
  -- token meta is {string,quote rule}
  local tokens, token = {}, {}
  local escaped, start = false, -1
  for i = 1, unicode.len(input) do
    local char = unicode.sub(input, i, i)
    if escaped then -- escaped character
      escaped = false
      -- include escape char if show_escapes
      -- or the followwing are all true
      -- 1. qr active
      -- 2. the char escaped is NOT the qr closure
      -- 3. qr is not literal
      if show_escapes or (qr and not qr[3] and qr[2] ~= char) then
        append(token, '\\', qr)
      end
      append(token, char, qr)
    elseif char == "\\" and (not qr or not qr[3]) then
        escaped = true
    elseif qr and qr[2] == char then -- end of quoted string
      -- if string is empty, we can still capture a quoted empty arg
      if #token == 0 or #token[#token] == 0 then
        append(token, '', qr)
      end
      qr = nil
    elseif not qr and tx.first(quotes,function(Q)
      qr=Q[1]==char and Q or nil return qr end) then
      start = i
    elseif not qr and string.find(char, "%s") then
      if #token > 0 then
        table.insert(tokens, token)
      end
      token = {}
    else -- normal char
      append(token, char, qr)
    end
  end
  if qr then
    return nil, "unclosed quote at index " .. start
  end

  if #token > 0 then
    table.insert(tokens, token)
  end

  return tokens
end

-- splits each word into words at delimiters
-- delimiters are kept as their own words
-- quoted word parts are not split
function --[[@delayloaded-start@]] text.internal.splitWords(words, delimiters)
  checkArg(1,words,"table")
  checkArg(2,delimiters,"table")

  local split_words = {}
  local next_word
  local function add_part(part)
    if next_word then
      split_words[#split_words+1] = {}
    end
    table.insert(split_words[#split_words], part)
    next_word = false
  end
  for wi=1,#words do local word = words[wi]
    next_word = true
    for pi=1,#word do local part = word[pi]
      local qr = part.qr
      if qr then
        add_part(part)
      else
        local part_text_splits = text.split(part.txt, delimiters)
        tx.foreach(part_text_splits, function(sub_txt, spi)
          local delim = #text.split(sub_txt, delimiters, true) == 0
          next_word = next_word or delim
          add_part({txt=sub_txt,qr=qr})
          next_word = delim
        end)
      end
    end
  end

  return split_words
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] text.internal.normalize(words, omitQuotes)
  checkArg(1, words, "table")
  checkArg(2, omitQuotes, "boolean", "nil")
  local norms = {}
  for _,word in ipairs(words) do
    local norm = {}
    for _,part in ipairs(word) do
      norm = tx.concat(norm, not omitQuotes and part.qr and {part.qr[1], part.txt, part.qr[2]} or {part.txt})
    end
    norms[#norms+1]=table.concat(norm)
  end
  return norms
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] text.internal.seeker(handle, whence, to)
  if not handle.txt then
    return nil, "bad file descriptor"
  end
  to = to or 0
  if whence == "cur" then
    handle.offset = handle.offset + to
  elseif whence == "set" then
    handle.offset = to
  elseif whence == "end" then
    handle.offset = handle.len + to
  end
  handle.offset = math.max(0, math.min(handle.offset, handle.len))
  return handle.offset
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] text.internal.reader(txt)
  checkArg(1, txt, "string")
  local reader =
  {
    txt = txt,
    len = unicode.len(txt),
    offset = 0,
    read = function(_, n)
      checkArg(1, n, "number")
      if not _.txt then
        return nil, "bad file descriptor"
      end
      if _.offset >= _.len then
        return nil
      end
      local last_offset = _.offset
      _:seek("cur", n)
      local next = unicode.sub(_.txt, last_offset + 1, _.offset)
      return next
    end,
    seek = text.internal.seeker,
    close = function(_)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      _.txt = nil
      return true
    end,
  }

  return require("buffer").new("r", reader)
end --[[@delayloaded-end@]]

function --[[@delayloaded-start@]] text.internal.writer(ostream, append_txt)
  if type(ostream) == "table" then
    local mt = getmetatable(ostream) or {}
    checkArg(1, mt.__call, "function")
  end
  checkArg(1, ostream, "function", "table")
  checkArg(2, append_txt, "string", "nil")
  local writer =
  {
    txt = "",
    offset = 0,
    len = 0,
    write = function(_, ...)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      local pre, vs, pos = unicode.sub(_.txt, 1, _.offset), {}, unicode.sub(_.txt, _.offset + 1)
      for i,v in ipairs({...}) do
        table.insert(vs, v)
      end
      vs = table.concat(vs)
      _:seek("cur", unicode.len(vs))
      _.txt = pre .. vs .. pos
      _.len = unicode.len(_.txt)
      return true
    end,
    seek = text.internal.seeker,
    close = function(_)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      ostream((append_txt or "") .. _.txt)
      _.txt = nil
      return true
    end,
  }

  return require("buffer").new("w", writer)
end --[[@delayloaded-end@]]

return text, local_env
