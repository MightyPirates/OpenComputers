local fs = require("filesystem")
local shell = require("shell")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: cp [-inrv] <from...> <to>\n")
  io.write(" -i: prompt before overwrite (overrides -n option).\n")
  io.write(" -n: do not overwrite an existing file.\n")
  io.write(" -r: copy directories recursively.\n")
  io.write(" -u: copy only when the SOURCE file differs from the destination\n")
  io.write("     file or when the destination file is missing.\n")
  io.write(" -v: verbose output.\n")
  io.write(" -x: stay on original source file system.\n")
  return 1
end

local from = {}
for i = 1, #args - 1 do
  table.insert(from, shell.resolve(args[i]))
end
local to = shell.resolve(args[#args])

local function status(from, to)
  if options.v then
    io.write(from .. " -> " .. to .. "\n")
  end
  os.sleep(0) -- allow interrupting
end

local result, reason

local function prompt(message)
  io.write(message .. " [Y/n] ")
  local result = io.read()
  return result and (result == "" or result:sub(1, 1):lower() == "y")
end

local function areEqual(path1, path2)
  local f1 = io.open(path1, "rb")
  if not f1 then
    return nil, "could not open `" .. path1 .. "' for update test"
  end
  local f2 = io.open(path2, "rb")
  if not f2 then
    f1:close()
    return nil, "could not open `" .. path2 .. "' for update test"
  end
  local result = true
  local chunkSize = 4 * 1024
  repeat
    local s1, s2 = f1:read(chunkSize), f2:read(chunkSize)
    if s1 ~= s2 then
      result = false
      break
    end
  until not s1 or not s2
  f1:close()
  f2:close()
  return result
end

local mounts = {}
for dev,path in fs.mounts() do
  mounts[fs.canonical(path)] = dev
end

local function recurse(fromPath, toPath, origin)
  status(fromPath, toPath)
  if fs.isDirectory(fromPath) then
    if not options.r then
      io.write("omitting directory `" .. fromPath .. "'\n")
      return true
    end
    if fs.exists(toPath) and not fs.isDirectory(toPath) then
      -- my real cp always does this, even with -f, -n or -i.
      return nil, "cannot overwrite non-directory `" .. toPath .. "' with directory `" .. fromPath .. "'"
    end
    if options.x and origin and mounts[fs.canonical(fromPath)] then
      return true
    end
    if fs.get(fromPath) == fs.get(toPath) and fs.canonical(fs.path(toPath)):find(fs.canonical(fromPath),1,true)  then
      return nil, "cannot copy a directory, `" .. fromPath .. "', into itself, `" .. toPath .. "'"
    end
    fs.makeDirectory(toPath)
    for file in fs.list(fromPath) do
      local result, reason = recurse(fs.concat(fromPath, file), fs.concat(toPath, file), origin or fs.get(fromPath))
      if not result then
        return nil, reason
      end
    end
    return true
  else
    if fs.exists(toPath) then
      if fs.canonical(fromPath) == fs.canonical(toPath) then
        return nil, "`" .. fromPath .. "' and `" .. toPath .. "' are the same file"
      end
      if fs.isDirectory(toPath) then
        if options.i then
          if not prompt("overwrite `" .. toPath .. "'?") then
            return true
          end
        elseif options.n then
          return true
        else -- yes, even for -f
          return nil, "cannot overwrite directory `" .. toPath .. "' with non-directory"
        end
      else
        if options.u then
          if areEqual(fromPath, toPath) then
            return true
          end
        end
        if options.i then
          if not prompt("overwrite `" .. toPath .. "'?") then
            return true
          end
        elseif options.n then
          return true
        end
        -- else: default to overwriting
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
    if reason then
      io.stderr:write(reason..'\n')
    end
    return 1
  end
end
