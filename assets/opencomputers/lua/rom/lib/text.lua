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
  if not value or unicode.len(value) == 0 then
    return string.rep(" ", length)
  else
    return value .. string.rep(" ", length - unicode.len(value))
  end
end

function text.padLeft(value, length)
  checkArg(1, value, "string", "nil")
  checkArg(2, length, "number")
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

_G.text = text