local fs = require("filesystem")
local shell = require("shell")
local computer = require("computer")

local args, options = shell.parse(...)
if #args < 2 then
  io.write("Usage: cp [-inrv] <from...> <to>\n")
  io.write(" -i: prompt before overwrite (overrides -n option).\n")
  io.write(" -n: do not overwrite an existing file.\n")
  io.write(" -r: copy directories recursively.\n")
  io.write(" -u: copy only when the SOURCE file differs from the destination\n")
  io.write("     file or when the destination file is missing.\n")
  io.write(" -P: preserve attributes, e.g. symbolic links.\n")
  io.write(" -v: verbose output.\n")
  io.write(" -x: stay on original source file system.\n")
  return 1
end

local exit_code = nil
options.P = options.P or options.r

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
  if not result then -- closed pipe
    os.exit(1)
  end
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
  local isLink, target = fs.isLink(fromPath)
  local toIsLink, toLinkTarget = fs.isLink(toPath)
  local same_path = fs.canonical(isLink and target or fromPath) == fs.canonical(toIsLink and toLinkTarget or toPath)
  local same_link = isLink and toIsLink and same_path
  local toExists = fs.exists(toPath)
  
  if isLink and options.P and not (toExists and same_path and not toIsLink) then
    if toExists and options.n then
      return true
    end
    fs.remove(toPath)
    if toExists and options.v then
      io.write(string.format("removed '%s'\n", toPath))
    end
    status(fromPath, toPath)
    return fs.link(target, toPath)
  elseif fs.isDirectory(fromPath) then
    if not options.r then
      io.write("omitting directory `" .. fromPath .. "'\n")
      exit_code = 1
      return true
    end
    if fs.exists(toPath) and not fs.isDirectory(toPath) then
      -- my real cp always does this, even with -f, -n or -i.
      return nil, "cannot overwrite non-directory `" .. toPath .. "' with directory `" .. fromPath .. "'"
    end
    if options.x and origin and mounts[fs.canonical(fromPath)] then
      return true
    end
    if fs.get(fromPath) == fs.get(toPath) and (fs.canonical(toPath).."/"):find(fs.canonical(fromPath).."/",1,true)  then
      return nil, "cannot copy a directory, `" .. fromPath .. "', into itself, `" .. toPath .. "'"
    end
    if not fs.exists(toPath) then
      status(fromPath, toPath)
      fs.makeDirectory(toPath)
    end
    for file in fs.list(fromPath) do
      local result, reason = recurse(fs.concat(fromPath, file), fs.concat(toPath, file), origin or fs.get(fromPath))
      if not result then
        return nil, reason
      end
    end
    return true
  elseif fs.exists(fromPath) then
    if toExists then
      if same_path then
        return nil, "`" .. fromPath .. "' and `" .. toPath .. "' are the same file"
      end

      if options.n then
        return true
      end

      -- if target is link, we are updating the target
      if toIsLink then
        toPath = toLinkTarget
      end

      if options.u and not fs.isDirectory(toPath) and areEqual(fromPath, toPath) then
        return true
      end

      if options.i then
        if not prompt("overwrite `" .. toPath .. "'?") then
          return true
        end
      end

      if fs.isDirectory(toPath) then
        return nil, "cannot overwrite directory `" .. toPath .. "' with non-directory"
      end

      fs.remove(toPath)
    end
    status(fromPath, toPath)
    return fs.copy(fromPath, toPath)
  else
    return nil, "`" .. fromPath .. "': No such file or directory"
  end
end

local to = shell.resolve(args[#args])

for i = 1, #args - 1 do
  local arg = args[i]
  local fromPath = shell.resolve(arg)
  -- a "contents of" copy is where src path ends in . or ..
  -- a source path ending with . is not sufficient - could be the source filename
  local contents_of = arg:match("%.$") and not fromPath:match("%.$")
  local toPath = to
  -- we do not append fromPath name to toPath in case of contents_of copy
  if not contents_of and fs.isDirectory(toPath) then
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

return exit_code
