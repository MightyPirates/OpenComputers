local unicode = require("unicode")
local tx = require("transforms")

local text = {}
text.internal = {}

text.syntax = {"^%d?>>?&%d+$","^%d?>>?",">>?","<%&%d+","<",";","&&","||?"}

function text.trim(value) -- from http://lua-users.org/wiki/StringTrim
  local from = string.match(value, "^%s*()")
  return from > #value and "" or string.match(value, ".*%S", from)
end

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

-- used by lib/sh
function text.escapeMagic(txt)
  return txt:gsub('[%(%)%.%%%+%-%*%?%[%^%$]', '%%%1')
end

function text.removeEscapes(txt)
  return txt:gsub("%%([%(%)%.%%%+%-%*%?%[%^%$])","%1")
end

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

require("package").delay(text, "/opt/core/full_text.lua")
require("package").delay(text.internal, "/opt/core/full_text.lua")

return text
