local fs = require("filesystem")
local shell = require("shell")
local lib = {}

local function perr(ops, format, ...)
  if format then
    io.stderr:write(ops.cmd .. string.format(": " .. format, ...) .. "\n")
    ops.exit_code = 1
    return 1
  end
end

local function contents_check(arg, options, bMustExist)
  if arg == "" then
    return perr(options, "cannot create regular file '' No such file or directory")
  end
  local path = shell.resolve(arg)
  local content_pattern = "^(%.*)(.?)"
  local contents_of, of_dir = arg:reverse():match(content_pattern)
  of_dir = of_dir:match("^/?$")
  local dots = contents_of and contents_of:len() or 0
  contents_of = of_dir and ({true,true})[dots]

  if (not bMustExist or fs.exists(path)) and of_dir and not fs.isDirectory(path) then
    perr(options, "'%s' is not a directory", arg)
    os.exit(1)
  end

  return contents_of, path
end

local function areEqual(path1, path2)
  local f1, f2 = fs.open(path1, "rb")
  if f1 then
    f2 = fs.open(path2, "rb")
    if f2 then
      local result = true
      local chunkSize = 4 * 1024
      repeat
        local s1, s2 = f1:read(chunkSize), f2:read(chunkSize)
        if s1 ~= s2 then
          result = false
          break
        end
      until not s1 or not s2
      f2:close()
    end
    f1:close()
  end
  assert(f1 and f2, "could not open files for reading: " .. path1 .. ", " .. path2)
  return result
end

local function status(verbose, from, to)
  if verbose then
    to = to and (" -> " .. to) or ""
    io.write(from .. to .. "\n")
  end
  os.sleep(0) -- allow interrupting
end

local function prompt(message)
  io.write(message .. " [Y/n] ")
  local result = io.read()
  if not result then -- closed pipe
    os.exit(1)
  end
  return result and (result == "" or result:sub(1, 1):lower() == "y")
end

local function stat(path, ops, P)
  local real, reason = fs.realPath(path)
  if not real and not P then
    perr(ops, "cannot read '%s': '%s'", path, reason)
    return false
  end
  local isLink, linkTarget = fs.isLink(path)
  return true,
         real,
         reason,
         isLink,
         linkTarget,
         fs.exists(path),
         fs.get(path),
         real and fs.isDirectory(real)
end

function lib.recurse(fromPath, toPath, options, origin, top)
  local mv = options.cmd == "mv"
  local function release(result, reason)
    if result and mv and top then
      local rm_result = not fs.get(fromPath).isReadOnly() and fs.remove(fromPath)
      if not rm_result then
        perr(options, "cannot remove '%s': filesystem is readonly", fromPath)
        result = false
      end
    end
    return result, reason
  end

  local
    ok,
    fromReal,
    fromError,
    fromIsLink,
    fromLinkTarget,
    fromExists,
    fromFs,
    fromIsDir = stat(fromPath, options, options.P)
  if not ok then return nil end
  local
    ok,
    toReal,
    toError,
    toIsLink,
    toLinkTarget,
    toExists,
    toFs,
    toIsDir = stat(toPath, options)
  if not ok then os.exit(1) end
  if toFs.isReadOnly() then
    perr(options, "cannot create target '%s': filesystem is readonly", toPath)
    return
  end

  local same_path = fromReal == toReal
  local same_link = fromIsLink and toIsLink and same_path

  local verbose = options.v
  local same_fs = fromFs == toFs
  local is_mount = origin[fromReal]

  if mv and is_mount then
    return false, string.format("cannot move '%s', it is a mount point", fromPath)
  end
  
  if fromIsLink and options.P and not (toExists and same_path and not toIsLink) then
    if toExists and options.n then
      return true
    end
    fs.remove(toPath)
    if toExists then
      status(verbose, string.format("removed '%s'\n", toPath))
    end
    status(verbose, fromPath, toPath)
    return release(fs.link(fromLinkTarget, toPath))
  elseif fromIsDir then
    if not options.r then
      status(true, string.format("omitting directory '%s'", fromPath))
      options.exit_code = 1
      return true
    end
    if toExists and not toIsDir then
      -- my real cp always does this, even with -f, -n or -i.
      return nil, "cannot overwrite non-directory '" .. toPath .. "' with directory '" .. fromPath .. "'"
    end
    if options.x and not top and is_mount then
      return true
    end
    if same_fs then
      if (toReal.."/"):find(fromReal.."/",1,true) then
        return nil, "cannot write a directory, '" .. fromPath .. "', into itself, '" .. toPath .. "'"
      elseif mv then
        return os.rename(fromPath, toPath)
      end
    end
    if not toExists then
      status(verbose, fromPath, toPath)
      fs.makeDirectory(toPath)
    end
    for file in fs.list(fromPath) do
      local result, reason = lib.recurse(fs.concat(fromPath, file), fs.concat(toPath, file), options, origin, false) -- false, no longer top
      if not result then
        return false, reason
      end
    end
    return release(true)
  elseif fromExists then
    if toExists then
      if same_path then
        return nil, "'" .. fromPath .. "' and '" .. toPath .. "' are the same file"
      end
      if options.n then
        return true
      end
      if options.u and not toIsDir and areEqual(fromReal, toReal) then
        return true
      end
      if options.i then
        if not prompt("overwrite '" .. toPath .. "'?") then
          return true
        end
      end
      if toIsDir then
        return nil, "cannot overwrite directory '" .. toPath .. "' with non-directory"
      end
      fs.remove(toReal)
    end
    status(verbose, fromPath, toPath)
    return release(fs.copy(fromPath, toPath))
  else
    return nil, "'" .. fromPath .. "': No such file or directory"
  end
end

function lib.batch(args, options)
  options.exit_code = 0

  -- standardized options
  options.i = options.i and not options.f
  options.P = options.P or options.r
  
  local origin = {}
  for dev,path in fs.mounts() do
    origin[path] = dev
  end

  local toArg = table.remove(args)
  local _, toPath = contents_check(toArg, options)
  if not toPath then
    return 1
  end
  local originalToIsDir = fs.isDirectory(toPath)

  for _,arg in ipairs(args) do
    -- a "contents of" copy is where src path ends in . or ..
    -- a source path ending with . is not sufficient - could be the source filename
    local contents_of, fromPath = contents_check(arg, options, true)
    if fromPath then
      -- we do not append fromPath name to toPath in case of contents_of copy
      local nextPath = toPath
      if contents_of and options.cmd == "mv" then
        perr(options, "invalid move path '%s'", arg)
      else
        if not contents_of and originalToIsDir then
          nextPath = fs.concat(nextPath, fs.name(fromPath))
        end

        local result, reason = lib.recurse(fromPath, nextPath, options, origin, true)

        if not result then
          perr(options, reason)
        end
      end
    end
  end

  return options.exit_code
end

return lib