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

function text.tokenize(value)
  checkArg(1, value, "string")
  local tokens, token = {}, ""
  local quoted, start, escaped = false, -1, false
  for i = 1, unicode.len(value) do
    local char = unicode.sub(value, i, i)
    if escaped then -- escaped character
      local f = load("return '\\" .. char .. "'")
      if f then
        local ok, f = pcall(f)
        if ok then
          char = f
        end
      end
      escaped = false
      token = token .. char
    elseif char == quoted then -- end of quoted string
      quoted = false
    elseif char == "\\" then -- escape character?
      escaped = true
    elseif (char == "'" or char == '"') and not quoted then
      quoted = char
      start = i
    elseif string.find(char, "%s") and not quoted then -- delimiter
      if token ~= "" then
        table.insert(tokens, token)
        token = ""
      end
    else -- normal char
      token = token .. char
      escaped = false
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

-------------------------------------------------------------------------------

-- Important: pretty formatting will allow presenting non-serializable values
-- but may generate output that cannot be unserialized back.
function text.serialize(value, pretty)
  local kw =  {["and"]=true, ["break"]=true, ["do"]=true, ["else"]=true,
               ["elseif"]=true, ["end"]=true, ["false"]=true, ["for"]=true,
               ["function"]=true, ["goto"]=true, ["if"]=true, ["in"]=true,
               ["local"]=true, ["nil"]=true, ["not"]=true, ["or"]=true,
               ["repeat"]=true, ["return"]=true, ["then"]=true, ["true"]=true,
               ["until"]=true, ["while"]=true}
  local id = "^[%a_][%w_]*$"
  local ts = {}
  local function s(v, l)
    local t = type(v)
    if t == "nil" then
      return "nil"
    elseif t == "boolean" then
      return v and "true" or "false"
    elseif t == "number" then
      if v ~= v then
        return "0/0"
      elseif v == math.huge then
        return "math.huge"
      elseif v == -math.huge then
        return "-math.huge"
      else
        return tostring(v)
      end
    elseif t == "string" then
      return string.format("%q", v)
    elseif t == "table" and pretty and getmetatable(v) and getmetatable(v).__tostring then
      return tostring(v)
    elseif t == "table" then
      if ts[v] then
        if pretty then
          return "recursion"
        else
          error("tables with cycles are not supported")
        end
      end
      ts[v] = true
      local f, i, r = 1, nil
      if pretty then
        local ks = {}
        for k in pairs(v) do table.insert(ks, k) end
        table.sort(ks)
        local k = 0
        f = function()
          k = k + 1
          return ks[k], ks[k] and v[ks[k]] or nil
        end
      else
        f = pairs(v)
      end
      for k, v in f do
        if r then
          r = r .. "," .. (pretty and ("\n" .. string.rep(" ", l)) or "")
        else
          r = "{"
        end
        local tk = type(k)
        if tk == "number" and k == i then
          i = i + 1
          r = r .. s(v, l + 1)
        else
          if tk == "string" and not kw[k] and string.match(k, id) then
            r = r .. k
          else
            r = r .. "[" .. s(k, l + 1) .. "]"
          end
          r = r .. "=" .. s(v, l + 1)
        end
      end
      ts[v] = nil -- allow writing same table more than once
      return (r or "{") .. "}"
    else
      if pretty then
        return tostring(t)
      else
        error("unsupported type: " .. t)
      end
    end
  end
  local result = s(value, 1)
  local limit = type(pretty) == "number" and pretty or 1000
  if pretty and unicode.len(result) > limit then
    return result:sub(1, limit) .. "..."
  end
  return result
end

function text.unserialize(data)
  checkArg(1, data, "string")
  local result, reason = load("return " .. data, "=data", _, {math={huge=math.huge}})
  if not result then
    return nil, reason
  end
  local ok, output = pcall(result)
  if not ok then
    return nil, output
  end
  return output
end

-------------------------------------------------------------------------------

return text
