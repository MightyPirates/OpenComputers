local component = require("component")
local package = require("package")
local term = require("term")
local text = require("text")

local function optrequire(...)
  local success, module = pcall(require, ...)
  if success then
    return module
  end
end

local history = {}
local env = setmetatable({}, {__index = function(t, k)
  return _ENV[k] or optrequire(k)
end})

component.gpu.setForeground(0xFFFFFF)
term.write("Lua 5.2.3 Copyright (C) 1994-2013 Lua.org, PUC-Rio\n")
component.gpu.setForeground(0xFFFF00)
term.write("Enter a statement and hit enter to evaluate it.\n")
term.write("Prefix an expression with '=' to show its value.\n")
term.write("Press Ctrl+C to exit the interpreter.\n")
component.gpu.setForeground(0xFFFFFF)

while term.isAvailable() do
  local foreground = component.gpu.setForeground(0x00FF00)
  term.write(tostring(env._PROMPT or "lua> "))
  component.gpu.setForeground(foreground)
  local command = term.read(history)
  if command == nil then -- eof
    return
  else
    if not (string.find(command,"=") or string.find(command,"end")) then
      command = "term.write("..command..")"
    end
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
    local result = table.pack(xpcall(code, debug.traceback))
    if not result[1] then
      if type(result[2]) == "table" and result[2].reason == "terminated" then
        os.exit(result[2].code)
      end
      io.stderr:write(tostring(result[2]) .. "\n")
    else
      for i = 2, result.n do
        term.write(text.serialize(result[i], true) .. "\t", true)
      end
      if term.getCursor() > 1 then
        term.write("\n")
      end
    end
  else
    io.stderr:write(tostring(reason) .. "\n")
  end
end
