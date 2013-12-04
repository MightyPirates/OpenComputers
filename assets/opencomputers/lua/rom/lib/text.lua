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

-------------------------------------------------------------------------------

function text.serialize(value)
  local kw =  {["and"]=true, ["break"]=true, ["do"]=true, ["else "]=true,
               ["elseif"]=true, ["end"]=true, ["false"]=true, ["for"]=true,
               ["function"]=true, ["goto "]=true, ["if"]=true, ["in"]=true,
               ["local"]=true, ["nil"]=true, ["not"]=true, ["or "]=true,
               ["repeat"]=true, ["return"]=true, ["then"]=true, ["true"]=true,
               ["until"]=true, ["while"]=true}
  local id = "[%a_][%w_]*"
  local ts = {}
  local function s(v)
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
    elseif t == "table" then
      if ts[v] then
        error("tables with cycles are not supported")
      end
      ts[v] = true
      local i, r = 1, nil
      for k, v in pairs(v) do
        if r then
          r = r .. ","
        else
          r = "{"
        end
        local tk = type(k)
        if tk == "number" and k == i then
          i = i + 1
          r = r .. s(v)
        else
          if tk == "string" and not kw[tk] and string.match(id, k) then
            r = r .. k
          else
            r = r .. "[" .. s(k) .. "]"
          end
          r = r .. "=" .. s(v)
        end
      end
      ts[v] = false -- allow writing same table more than once
      return (r or "{") .. "}"
    else
      error("unsupported type: " .. t)
    end
  end
  return s(value)
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

_G.text = text