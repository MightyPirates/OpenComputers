local component = require("component")
local package = require("package")
local term = require("term")
local text = require("text")

local history = {}
local env = setmetatable({}, {__index = function(t, k)
  return _ENV[k] or package.loaded[k]
end})

print("Lua 5.2.3 Copyright (C) 1994-2013 Lua.org, PUC-Rio")

while term.isAvailable() do
  local foreground = component.gpu.setForeground(0x00FF00)
  term.write("lua> ")
  component.gpu.setForeground(foreground)
  local command = term.read(history)
  if command == nil then -- eof
    return
  end
  while #history > 10 do
    table.remove(history, 1)
  end
  local statement, result = load(command, "=stdin", "t", env)
  local expression = load("return " .. command, "=stdin", "t", env)
  local code = expression or statement
  if code then
    local result = table.pack(pcall(code))
    if not result[1] then
      print(result[2])
    else
      for i = 1, result.n do
        result[i] = text.serialize(result[i], true)
      end
      print(table.unpack(result, 2, result.n))
    end
  else
    print(result)
  end
end
