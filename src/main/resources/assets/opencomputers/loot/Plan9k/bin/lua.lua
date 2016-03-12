local package = require("package")
local term = require("term")
local serialization = require("serialization")
local shell = require("shell")

local args, options = shell.parse(...)
local env = setmetatable({}, {__index = _ENV})

if #args > 0 then
  local script, reason = loadfile(args[1], nil, env)
  if not script then
    io.stderr:write(tostring(reason) .. "\n")
    os.exit(false)
  end
  local result, reason = pcall(script, table.unpack(args, 2))
  if not result then
    io.stderr:write(reason)
    os.exit(false)
  end
end

if #args == 0 or options.i then
  local function optrequire(...)
    local success, module = pcall(require, ...)
    if success then
      return module
    end
  end
  setmetatable(env, {
    __index = function(t, k)
      _ENV[k] = _ENV[k] or optrequire(k)
      return _ENV[k]
    end,
    __pairs = function(self)
      local t = self
      return function(_, key)
        local k, v = next(t, key)
        if not k and t == env then
          t = _ENV
          k, v = next(t)
        end
        if not k and t == _ENV then
          t = package.loaded
          k, v = next(t)
        end
        return k, v
      end
    end
  })

  local history = {}

  local function findTable(t, path)
    if type(t) ~= "table" then return nil end
    if not path or #path == 0 then return t end
    local name = string.match(path, "[^.]+")
    for k, v in pairs(t) do
      if k == name then
        return findTable(v, string.sub(path, #name + 2))
      end
    end
    local mt = getmetatable(t)
    if t == env then mt = {__index=_ENV} end
    if mt then
      return findTable(mt.__index, path)
    end
    return nil
  end
  local function findKeys(t, r, prefix, name)
    if type(t) ~= "table" then return end
    for k, v in pairs(t) do
      if string.match(k, "^"..name) then
        local postfix = ""
        if type(v) == "function" then postfix = "()"
        elseif type(v) == "table" and getmetatable(v) and getmetatable(v).__call then postfix = "()"
        elseif type(v) == "table" then postfix = "."
        end
        r[prefix..k..postfix] = true
      end
    end
    local mt = getmetatable(t)
    if t == env then mt = {__index=_ENV} end
    if mt then
      return findKeys(mt.__index, r, prefix, name)
    end
  end
  local function hint(line, index)
    line = (line or ""):sub(1, index - 1)
    local path = string.match(line, "[a-zA-Z_][a-zA-Z0-9_.]*$")
    if not path then return nil end
    local suffix = string.match(path, "[^.]+$") or ""
    local prefix = string.sub(path, 1, #path - #suffix)
    local t = findTable(env, prefix)
    if not t then return nil end
    local r1, r2 = {}, {}
    findKeys(t, r1, string.sub(line, 1, #line - #suffix), suffix)
    for k in pairs(r1) do
      table.insert(r2, k)
    end
    table.sort(r2)
    return r2
  end

  --component.gpu.setForeground(0xFFFFFF)
  term.write(_VERSION .. " Copyright (C) 1994-2015 Lua.org, PUC-Rio\n")
  --component.gpu.setForeground(0xFFFF00)
  term.write("Enter a statement and hit enter to evaluate it.\n")
  term.write("Prefix an expression with '=' to show its value.\n")
  term.write("Press Ctrl+C to exit the interpreter.\n")
  --component.gpu.setForeground(0xFFFFFF)

  while term.isAvailable() do
    --local foreground = component.gpu.setForeground(0x00FF00)
    term.write(tostring(env._PROMPT or "lua> "))
    --component.gpu.setForeground(foreground)
    local command = term.read(history, nil, hint)
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
      local result = table.pack(xpcall(code, debug.traceback))
      if not result[1] then
        if type(result[2]) == "table" and result[2].reason == "terminated" then
          os.exit(result[2].code)
        end
        io.stderr:write(tostring(result[2]) .. "\n")
      else
        for i = 2, result.n do
          term.write(serialization.serialize(result[i], 25) .. "\t", true)
        end
        if term.getCursor() > 1 then
          term.write("\n")
        end
      end
    else
      io.stderr:write(tostring(reason) .. "\n")
    end
  end
end