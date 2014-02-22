local unicode = require("unicode")

local text = {}

function text.detab(value, tabWidth)
  checkArg(1, value, "string")
  checkArg(2, tabWidth, "number", "nil")
  tabWidth = tabWidth or 4
  local function rep(match)
    local spaces = tabWidth - match:len() % tabWidth
    return match .. string.rep(" ", spaces)
  end
  return value:gsub("([^\n]-)\t", rep)
end

function text.padRight(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  local unicode = require("unicode")
  if not value or unicode.len(value) == 0 then
    return string.rep(" ", length)
  else
    return value .. string.rep(" ", length - unicode.len(value))
  end
end

function text.padLeft(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
  local unicode = require("unicode")
  if not value or unicode.len(value) == 0 then
    return string.rep(" ", length)
  else
    return string.rep(" ", length - unicode.len(value)) .. value
  end
end

function text.trim(value) -- from http://lua-users.org/wiki/StringTrim
  local from = string.match(value, "^%s*()")
  return from > #value and "" or string.match(value, ".*%S", from)
end

-------------------------------------------------------------------------------

function text.tokenize(value)
  checkArg(1, value, "string")
  local tokens, token = {}, ""
  local escaped, quoted, start = false, false, -1
  for i = 1, unicode.len(value) do
    local char = unicode.sub(value, i, i)
    if escaped then -- escaped character
      escaped = false
      token = token .. char
    elseif char == "\\" and quoted ~= "'" then -- escape character?
      escaped = true
      token = token .. char
    elseif char == quoted then -- end of quoted string
      quoted = false
      token = token .. char
    elseif (char == "'" or char == '"') and not quoted then
      quoted = char
      start = i
      token = token .. char
    elseif string.find(char, "%s") and not quoted then -- delimiter
      if token ~= "" then
        table.insert(tokens, token)
        token = ""
      end
    else -- normal char
      token = token .. char
    end
  end
  if quoted then
    return nil, "unclosed quote at index " .. start
  end
  if token ~= "" then
    table.insert(tokens, token)
  end
  return tokens
end

function text.serialize(value, pretty) -- deprecated, use serialization module
  return require("serialization").serialize(value, pretty)
end

function text.unserialize(data) -- deprecated, use serialization module
  return require("serialization").unserialize(data)
end

-------------------------------------------------------------------------------

return text
