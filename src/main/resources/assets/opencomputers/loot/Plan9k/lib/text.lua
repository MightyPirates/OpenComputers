local unicode = require("unicode")

local text = {}

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

local operators = {";", "&&", "||", "|"}
local function checkOp(string)
  for _, v in pairs(operators) do
    if unicode.sub(v, 1, unicode.len(string)) == string then
      return true
    end
  end
  return false
end

function text.tokenize(value)
  checkArg(1, value, "string")
  local tokens, token = {}, ""
  local escaped, quoted, start = false, false, -1
  local op = false
  for i = 1, unicode.len(value) do
    local char = unicode.sub(value, i, i)
    if escaped then -- escaped character
      escaped = false
      token = token..char
    else
      local newOp
      if op then
        newOp = token..char
      else
        newOp = char
      end
      if checkOp(newOp) then -- part of operator?
        if not op then -- delimit token if start of operator
          table.insert(tokens, token)
          op = true
        end
        token = newOp
      else
        if op then -- end of operator?
          local foundOp = false
          for _, v in pairs(operators) do
            if v == token then
              table.insert(tokens, token)
              foundOp = true
            end
          end
          if not foundOp then
            tokens[#tokens] = tokens[#tokens]..token
          end
          token = ""
          op = false
        end

        -- Continue with regular matching
        if char == "\\" and quoted ~= "'" then -- escape character?
          escaped = true
          token = token..char
        elseif char == quoted then -- end of quoted string
          quoted = false
          token = token..char
        elseif (char == "'" or char == '"' or char == '`') and not quoted then
          quoted = char
          start = i
          token = token..char
        elseif string.find(char, "%s") and not quoted then -- delimiter
          if token ~= "" then
            table.insert(tokens, token)
            token = ""
          end
        else -- normal char
          token = token..char
        end
      end
    end
  end
  if quoted then
    return nil, "unclosed quote at index " .. start, quoted
  end
  if token ~= "" then
    table.insert(tokens, token)
  end
  return tokens
end

-------------------------------------------------------------------------------

return text

