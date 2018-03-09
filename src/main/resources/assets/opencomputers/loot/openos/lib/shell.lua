local fs = require("filesystem")
local unicode = require("unicode")
local process = require("process")

local shell = {}

-- Cache loaded shells for command execution. This puts the requirement on
-- shells that they do not keep a global state, since they may be called
-- multiple times, but reduces memory usage a lot.
local shells = setmetatable({}, {__mode="v"})

function shell.getShell()
  local shellPath = os.getenv("SHELL") or "/bin/sh"
  local shellName, reason = shell.resolve(shellPath, "lua")
  if not shellName then
    return nil, "cannot resolve shell `" .. shellPath .. "': " .. reason
  end
  if shells[shellName] then
    return shells[shellName]
  end
  local sh, load_reason = loadfile(shellName, nil, setmetatable({}, {__index=_G}))
  if sh then
    shells[shellName] = sh
  end
  return sh, load_reason
end

-------------------------------------------------------------------------------

function shell.prime()
  local data = process.info().data
  for _,key in ipairs({'aliases','vars'}) do
    -- first time get need to populate
    local raw = rawget(data, key)
    if not raw then
      -- current process does not have the key
      local current = data[key]
      data[key] = {}
      if current then
        for k,v in pairs(current) do
          data[key][k] = v
        end
      end
    end
  end
end

function shell.getAlias(alias)
  return process.info().data.aliases[alias]
end

function shell.setAlias(alias, value)
  checkArg(1, alias, "string")
  checkArg(2, value, "string", "nil")
  process.info().data.aliases[alias] = value
end

function shell.getWorkingDirectory()
  -- if no env PWD default to /
  return os.getenv("PWD") or "/"
end

function shell.setWorkingDirectory(dir)
  checkArg(1, dir, "string")
  -- ensure at least /
  -- and remove trailing /
  dir = fs.canonical(dir):gsub("^$", "/"):gsub("(.)/$", "%1")
  if fs.isDirectory(dir) then
    os.setenv("PWD", dir)
    return true
  else
    return nil, "not a directory"
  end
end

function shell.resolve(path, ext)
  checkArg(1, path, "string")

  local dir = path
  if dir:find("/") ~= 1 then
    dir = fs.concat(shell.getWorkingDirectory(), dir)
  end
  local name = fs.name(path)
  dir = fs[name and "path" or "canonical"](dir)
  local fullname = fs.concat(dir, name or "")

  if not ext then
    return fullname
  elseif name then
    checkArg(2, ext, "string")
    -- search for name in PATH if no dir was given
    -- no dir was given if path has no /
    local search_in = path:find("/") and dir or os.getenv("PATH")
    for search_path in string.gmatch(search_in, "[^:]+") do
      -- resolve search_path because they may be relative
      local search_name = fs.concat(shell.resolve(search_path), name)
      if not fs.exists(search_name) then
        search_name = search_name .. "." .. ext
      end
      -- extensions are provided when the caller is looking for a file
      if fs.exists(search_name) and not fs.isDirectory(search_name) then
        return search_name
      end
    end
  end

  return nil, "file not found"
end

function shell.parse(...)
  local params = table.pack(...)
  local args = {}
  local options = {}
  local doneWithOptions = false
  for i = 1, params.n do
    local param = params[i]
    if not doneWithOptions and type(param) == "string" then
      if param == "--" then
        doneWithOptions = true -- stop processing options at `--`
      elseif param:sub(1, 2) == "--" then
        local key, value = param:match("%-%-(.-)=(.*)")
        if not key then
          key, value = param:sub(3), true
        end
        options[key] = value
      elseif param:sub(1, 1) == "-" and param ~= "-" then
        for j = 2, unicode.len(param) do
          options[unicode.sub(param, j, j)] = true
        end
      else
        table.insert(args, param)
      end
    else
      table.insert(args, param)
    end
  end
  return args, options
end

-------------------------------------------------------------------------------

require("package").delay(shell, "/lib/core/full_shell.lua")

return shell
