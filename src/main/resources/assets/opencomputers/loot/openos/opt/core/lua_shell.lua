local package = require("package")
local tty = require("tty")
local process = require("process")

local gpu = tty.gpu()

local function optrequire(...)
  local success, module = pcall(require, ...)
  if success then
    return module
  end
end

local env -- forward declare for binding in metamethod
env = setmetatable({}, {
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
  end,
})
env._PROMPT = tostring(env._PROMPT or "lua> ")

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
    if type(k) == "string" and string.match(k, "^"..name) then
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

local read_handler = {}
function read_handler.hint(line, index)
  line = (line or "")
  local tail = line:sub(index)
  line = line:sub(1, index - 1)
  local path = string.match(line, "[a-zA-Z_][a-zA-Z0-9_.]*$")
  if not path then return nil end
  local suffix = string.match(path, "[^.]+$") or ""
  local prefix = string.sub(path, 1, #path - #suffix)
  local tbl = findTable(env, prefix)
  if not tbl then return nil end
  local keys = {}
  local hints = {}
  findKeys(tbl, keys, string.sub(line, 1, #line - #suffix), suffix)
  for key in pairs(keys) do
    table.insert(hints, key .. tail)
  end
  return hints
end

gpu.setForeground(0xFFFFFF)
tty.write(_VERSION .. " Copyright (C) 1994-2015 Lua.org, PUC-Rio\n")
gpu.setForeground(0xFFFF00)
tty.write("Enter a statement and hit enter to evaluate it.\n")
tty.write("Prefix an expression with '=' to show its value.\n")
tty.write("Press Ctrl+D to exit the interpreter.\n")
gpu.setForeground(0xFFFFFF)

while tty.isAvailable() do
  local foreground = gpu.setForeground(0x00FF00)
  tty.write(env._PROMPT)
  gpu.setForeground(foreground)
  local command = tty.read(read_handler)
  if not command then -- eof
    return
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
        tty.write(require("serialization").serialize(result[i], true) .. "\t", true)
      end
      if tty.getCursor() > 1 then
        tty.write("\n")
      end
    end
  else
    io.stderr:write(tostring(reason) .. "\n")
  end
end
