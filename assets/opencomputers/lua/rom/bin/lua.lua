print("Lua 5.2.2 Copyright (C) 1994-2013 Lua.org, PUC-Rio")
local running = true
local env = setmetatable({exit=function() running = false end}, {__index=_ENV})
while running and term.isAvailable() do
  io.write("lua> ")
  local command = io.read()
  if not command then
    return -- eof
  end
  local statement, result = load(command, "=stdin", env)
  local expression = load("return " .. command, "=stdin", env)
  local code = expression or statement
  if code then
    local result = table.pack(pcall(code))
    if not result[1] or result.n > 1 then
      print(table.unpack(result, 2, result.n))
    end
  else
    print(result)
  end
end
