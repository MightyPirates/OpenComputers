local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: cp [-f] <from> <to>\n")
  io.write(" -f: overwrite file if it already exists.")
  return
end

local from = shell.resolve(args[1])
local to = shell.resolve(args[2])
if fs.isDirectory(to) then
  to = to .. "/" .. fs.name(from)
end

local function status(from, to)
  if options.v then
    print(from .. " -> " .. to)
  end
end

local result, reason

local function recurse(fromPath, toPath)
  status(fromPath, toPath)
  if fs.isDirectory(fromPath) then
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
      if fs.isDirectory(toPath) or not options.f then
        return nil, "target file exists"
      end
      fs.remove(toPath)
    end
    return fs.copy(fromPath, toPath)
  end
end
result, reason = recurse(from, to)
if not result then
  error(reason)
end