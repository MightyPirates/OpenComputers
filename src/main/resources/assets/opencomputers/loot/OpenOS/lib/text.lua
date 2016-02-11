local unicode = require("unicode")
local tx = require("transforms")

-- --[[@@]] are not just comments, but custom annotations for delayload methods.
-- See package.lua and the api wiki for more information

local text = {}
local local_env = {tx=tx,unicode=unicode}

text.internal = {}
setmetatable(text.internal,
{
  __tostring=function()
    return 'table of undocumented api subject to change and intended for internal use'
  end
})

text.syntax = {";","&&","||","|",">>",">","<"}

function text.detab(value, tabWidth)
  checkArg(1, value, "string")
  checkArg(2, tabWidth, "number", "nil")
  tabWidth = tabWidth or 8
  local function rep(match)
    local spaces = tabWidth - match:len() % tabWidth
    return match .. string.rep(" ", spaces)
  end
  local result = value:gsub("([^\n]-)\t", rep) -- truncate results
  return result
end

function text.padRight(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  if not value or unicode.wlen(value) == 0 then
    return string.rep(" ", length)
  else
    return value .. string.rep(" ", length - unicode.wlen(value))
  end
end

function text.padLeft(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  if not value or unicode.wlen(value) == 0 then
    return string.rep(" ", length)
  else
    return string.rep(" ", length - unicode.wlen(value)) .. value
  end
end

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

-- tokenize allows nil for delimiters, quotes, and doNotNormalize
-- always separates by whitespace
-- default quote rules: '' and ""
-- default delimiters: all
-- default is to normalize, that is, no metadata is returned, just a list of tokens
function text.tokenize(value, doNotNormalize, quotes, delimiters)
  checkArg(1, value, "string")
  checkArg(2, doNotNormalize, "boolean", "nil")
  checkArg(3, quotes, "table", "nil")
  checkArg(4, delimiters, "table", "nil")

  local tokens, reason = text.internal.tokenize(value, quotes, delimiters)

  if type(tokens) ~= "table" then
    return nil, reason
  end

  if doNotNormalize then
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

function text.internal.tokenize(value, quotes, delimiters)
  checkArg(1, value, "string")
  checkArg(2, quotes, "table", "nil")
  checkArg(3, delimiters, "table", "nil")
  delimiters = delimiters or text.syntax

  local words, reason = text.internal.words(value, quotes)

  local splitter = text.escapeMagic(table.concat(delimiters))
  if type(words) ~= "table" or 
    #splitter == 0 or
    not value:find("["..splitter.."]") then
    return words, reason
  end

  return text.internal.splitWords(words, delimiters)
end

function --[[@delayloaded-start@]] text.internal.table_view(str)
  checkArg(1, str, 'string')
  return setmetatable({s=str},
  { __index = function(_,k) return unicode.sub(_.s,k,k) end,
    __len = function(_) return unicode.len(_.s) end})
end --[[@delayloaded-end@]]

-- tokenize input by quotes and whitespace
function text.internal.words(input, quotes)
  checkArg(1, input, "string")
  checkArg(2, quotes, "table", "nil")
  local qr = nil
  quotes = quotes or {{"'","'",true},{'"','"'}}
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
      -- include escape char if
      -- 1. qr active
      -- 2. the char escaped is NOT the qr closure
      -- 3. qr is not literal
      if qr and not qr[3] and qr[2] ~= char then
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
  local delimLookup = tx.select(delimiters, function(e,i)
    return i, e
  end)
  for wi=1,#words do local word = words[wi]
    next_word = true
    for pi=1,#word do local part = word[pi]
      local qr = part.qr
      if qr then
        add_part(part)
      else
        local part_text_splits = text.split(part.txt, delimiters)
        tx.foreach(part_text_splits, function(sub_txt, spi)
          local delim = delimLookup[sub_txt]
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

return text, local_env
