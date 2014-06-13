local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: cp [-frv] <from...> <to>\n")
  io.write(" -f: overwrite file if it already exists.\n")
  io.write(" -r: copy directories recursively.\n")
  io.write(" -v: verbose output.")
  return
end

local from = {}
for i = 1, #args - 1 do
  table.insert(from, shell.resolve(args[i]))
end
local to = shell.resolve(args[#args])

local function status(from, to)
  if options.v then
    print(from .. " -> " .. to)
  end
  os.sleep(0) -- allow interrupting
end

local result, reason

local function recurse(fromPath, toPath)
  status(fromPath, toPath)
  if fs.isDirectory(fromPath) then
    if not options.r then
      io.write("omitting directory '" .. fromPath .. "'\n")
      return true
    end
    if fs.canonical(fromPath) == fs.canonical(fs.path(toPath)) then
      return nil, "cannot copy a directory, '" .. fromPath .. "', into itself, '" .. toPath .. "'\n"
    end
    if fs.exists(toPath) and not fs.isDirectory(toPath) then
      if not options.f then
        return nil, "target file exists"
      end
      fs.remove(toPath)
    end
    fs.makeDirectory(toPath)
    for file in fs.list(fromPath) do
      local result, reason = recurse(fs.concat(fromPath, file), fs.concat(toPath, file))
      if not result then
        return nil, reason
      end
    end
    return true
  else
    if fs.exists(toPath) then
      if fs.canonical(fromPath) == fs.canonical(toPath) then
        return nil, "'" .. fromPath .. "' and '" .. toPath .. "' are the same file"
      end
      if fs.isDirectory(toPath) or not options.f then
        return nil, "target file exists"
      end
      fs.remove(toPath)
    end
    return fs.copy(fromPath, toPath)
  end
end
for _, fromPath in ipairs(from) do
  local toPath = to
  if fs.isDirectory(toPath) then
    toPath = fs.concat(toPath, fs.name(fromPath))
  end
  result, reason = recurse(fromPath, toPath)
  if not result then
    error(reason, 0)
  end
end