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

local quotes = {
  -- array part must be ordered from long to short (irrelevant for now)
  '"', "'", "`";
  ['"']   = '"',
  ["'"]   = "'",
  ["`"]   = "`",
--["${"]  = "}",
--["$("]  = ")",
--["$(("] = "))"
}

function text.tokenize(value)
  checkArg(1, value, "string")

  local i = 0
  local len = unicode.len(value)

  local function consume(n)
    if type(n) == "string" then -- try to consume a predefined token
      local begin = i + 1
      local final = i + unicode.len(n)
      local sub = unicode.sub(value, begin, final)

      if sub == n then
        i = final
        return sub
      else
        return nil
      end
    elseif type(n) ~= "number" then -- for for loop iteration
      n = 1
    end
    if len - i <= 0 then
      return nil
    end
    n = math.min(n, len - i)
    local begin = i + 1
    i = i + n
    return unicode.sub(value, begin, i)
  end

  local function lookahead(n)
    if type(n) ~= "number" then
      n = 1
    end
    n = math.min(n, len - i)
    local begin = i + 1
    local final = i + n
    if final - i <= 0 then
      return nil
    end
    return unicode.sub(value, begin, final)
  end

  local tokens, token = {}, {}
  local start, quoted
  local lastOp
  while lookahead() do
    local char = lookahead()
    local closed
    if quoted then
      closed = consume(quotes[quoted])
    end
    if closed then
      table.insert(token, closed)
      quoted = nil
    elseif quoted == "'" then
      table.insert(token, consume(char))
    elseif char == "\\" then
      table.insert(token, consume(2)) -- backslashes will be removed later
    elseif not quoted then
      local opened
      for _, quote in ipairs(quotes) do
        opened = consume(quote)
        if opened then
          table.insert(token, opened)
          quoted = opened
          start = i + 1
          break
        end
      end
      if not opened then
        local isOp
        for _, op in ipairs(operators) do
          isOp = consume(op)
          if isOp then
            local tokenOut = table.concat(token)
            if tokenOut == "" then
              if tokens[#tokens] == lastOp then
                return nil, "parse error near '"..isOp.."'"
              end
            else
              table.insert(tokens, tokenOut)
            end
            table.insert(tokens, isOp)
            token = {}
            lastOp = isOp
            break
          end
        end
        if not isOp then
          if char == "#" then -- comment
            while lookahead() ~= "\n" do
              consume()
            end
          elseif char:match("%s") then
            local tokenOut = table.concat(token)
            if tokenOut ~= "" then
              table.insert(tokens, table.concat(token))
              token = {}
            end
            consume(char)
          else
            table.insert(token, consume(char))
          end
        end
      end
    else
      table.insert(token, consume(char))
    end
  end
  if quoted then
    return nil, "unclosed quote at index " .. start, quoted
  end
  token = table.concat(token)
  if token ~= "" then -- insert trailing token
    table.insert(tokens, token)
  end
  return tokens
end


-------------------------------------------------------------------------------

return text
