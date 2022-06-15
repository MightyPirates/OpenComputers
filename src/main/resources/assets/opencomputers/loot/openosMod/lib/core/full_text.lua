local text = require("text")
local tx = require("transforms")
local unicode = require("unicode")
local process = require("process")

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

-------------------------------------------------------------------------------
-- like tokenize, but does not drop any text such as whitespace
-- splits input into an array for sub strings delimited by delimiters
-- delimiters are included in the result if not dropDelims
function text.split(input, delimiters, dropDelims, di)
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
end

-----------------------------------------------------------------------------

-- splits each word into words at delimiters
-- delimiters are kept as their own words
-- quoted word parts are not split
function text.internal.splitWords(words, delimiters)
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
        tx.foreach(part_text_splits, function(sub_txt)
          local delim = #text.split(sub_txt, delimiters, true) == 0
          next_word = next_word or delim
          add_part({txt=sub_txt,qr=qr})
          next_word = delim
        end)
      end
    end
  end

  return split_words
end

function text.internal.normalize(words, omitQuotes)
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
end

function text.internal.stream_base(binary)
  return
  {
    binary = binary,
    plen = binary and string.len or unicode.len,
    psub = binary and string.sub or unicode.sub,
    seek = function (handle, whence, to)
      if not handle.txt then
        return nil, "bad file descriptor"
      end
      to = to or 0
      local offset = handle:indexbytes()
      if whence == "cur" then
        offset = offset + to
      elseif whence == "set" then
        offset = to
      elseif whence == "end" then
        offset = handle.len + to
      end
      offset = math.max(0, math.min(offset, handle.len))
      handle:byteindex(offset)
      return offset
    end,
    indexbytes = function (handle)
      return handle.psub(handle.txt, 1, handle.index):len()
    end,
    byteindex = function (handle, offset)
      local sub = string.sub(handle.txt, 1, offset)
      handle.index = handle.plen(sub)
    end,
  }
end

function text.internal.reader(txt, mode)
  checkArg(1, txt, "string")
  local reader = setmetatable(
  {
    txt = txt,
    len = string.len(txt),
    index = 0,
    read = function(_, n)
      checkArg(1, n, "number")
      if not _.txt then
        return nil, "bad file descriptor"
      end
      if _.index >= _.plen(_.txt) then
        return nil
      end
      local next = _.psub(_.txt, _.index + 1, _.index + n)
      _.index = _.index + _.plen(next)
      return next
    end,
    close = function(_)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      _.txt = nil
      return true
    end,
  }, {__index=text.internal.stream_base((mode or ""):match("b"))})
  process.closeOnExit(reader)

  return require("buffer").new("r", reader)
end

function text.internal.writer(ostream, mode, append_txt)
  if type(ostream) == "table" then
    local mt = getmetatable(ostream) or {}
    checkArg(1, mt.__call, "function")
  end
  checkArg(1, ostream, "function", "table")
  checkArg(2, append_txt, "string", "nil")
  local writer = setmetatable(
  {
    txt = "",
    index = 0, -- last location of write
    len = 0,
    write = function(_, ...)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      local pre = _.psub(_.txt, 1, _.index)
      local vs = {}
      local pos = _.psub(_.txt, _.index + 1)
      for _,v in ipairs({...}) do
        table.insert(vs, v)
      end
      vs = table.concat(vs)
      _.index = _.index + _.plen(vs)
      _.txt = pre .. vs .. pos
      _.len = string.len(_.txt)
      return true
    end,
    close = function(_)
      if not _.txt then
        return nil, "bad file descriptor"
      end
      ostream((append_txt or "") .. _.txt)
      _.txt = nil
      return true
    end,
  }, {__index=text.internal.stream_base((mode or ""):match("b"))})
  process.closeOnExit(writer)

  return require("buffer").new("w", writer)
end

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

function text.padLeft(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  if not value or unicode.wlen(value) == 0 then
    return string.rep(" ", length)
  else
    return string.rep(" ", length - unicode.wlen(value)) .. value
  end
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
  local line
  return function()
    if value then
      line, value = text.wrap(value, width, maxWidth)
      return line
    end
  end
end

