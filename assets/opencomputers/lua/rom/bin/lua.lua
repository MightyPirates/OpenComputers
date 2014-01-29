local component = require("component")
local package = require("package")
local term = require("term")
local text = require("text")

local history = {}
local env = setmetatable({}, {__index = function(t, k)
  return _ENV[k] or package.loaded[k]
end})

component.gpu.setForeground(0xFFFFFF)
print("Lua 5.2.3 Copyright (C) 1994-2013 Lua.org, PUC-Rio")
component.gpu.setForeground(0xFFFF00)
print("Enter a statement and hit enter to evaluate it.")
print("Prefix an expression with '=' to show its value.")
print("Press Ctrl+C to exit the interpreter.")
component.gpu.setForeground(0xFFFFFF)

while term.isAvailable() do
  local foreground = component.gpu.setForeground(0x00FF00)
  term.write(tostring(env._PROMPT or "lua> "))
  component.gpu.setForeground(foreground)
  local command = term.read(history)
  if command == nil then -- eof
    return
  end
  while #history > 10 do
    table.remove(history, 1)
  end
  local code, reason
  if string.sub(command, 1, 1) == "=" then
    code, reason = load("return " .. string.sub(command, 2), "=stdin", "t", env)
  else
    code, reason = load(command, "=stdin", "t", env)
  end
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
    print(reason)
  end
end
