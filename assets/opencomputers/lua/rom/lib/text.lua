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

_G.text = text