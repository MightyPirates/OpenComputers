local component = require("component")
local computer = require("computer")
local event = require("event")
local fs = require("filesystem")
local process = require("process")
local shell = require("shell")
local term = require("term")
local text = require("text")
local unicode = require("unicode")

local function expand(value)
  local result = value:gsub("%$(%w+)", os.getenv):gsub("%$%b{}",
    function(match) return os.getenv(expand(match:sub(3, -2))) or match end)
  return result
end

local function glob(value)
  if not value:find("*", 1, true) and not value:find("?", 1, true) then
    -- Nothing to do here.
    return {expand(value)}
  end
  local segments = fs.segments(value)
  local paths = {value:sub(1, 1) == "/" and "/" or shell.getWorkingDirectory()}
  for i, segment in ipairs(segments) do
    local nextPaths = {}
    local pattern = segment:gsub("*", ".*"):gsub("?", ".")
    if pattern == segment then
      -- Nothing to do, concatenate as-is.
      for _, path in ipairs(paths) do
        table.insert(nextPaths, fs.concat(path, segment))
      end
    else
      pattern = "^(" .. pattern .. ")/?$"
      for _, path in ipairs(paths) do
        for file in fs.list(path) do
          if file:match(pattern) then
            table.insert(nextPaths, fs.concat(path, file))
          end
        end
      end
      if #nextPaths == 0 then
        error("no matches found: " .. segment)
      end
    end
    paths = nextPaths
  end
  for i, path in ipairs(paths) do
    paths[i] = expand(path)
  end
  return paths
end

local function evaluate(value)
  local init, results = 1, {""}
  repeat
    local match = value:match("^%b''", init)
    if match then -- single quoted string. no variable expansion.
      match = match:sub(2, -2)
      init = init + 2
      for i, result in ipairs(results) do
        results[i] = result .. match
      end
    else
      match = value:match('^%b""', init)
      if match then -- double quoted string.
        match = match:sub(2, -2)
        init = init + 2
      else
        -- plaintext?
        match = value:match("^([^']+)%b''", init)
        if not match then -- unmatched single quote.
          match = value:match('^([^"]+)%b""', init)
          if not match then -- unmatched double quote.
            match = value:sub(init)
          end
        end
      end
      local newResults = {}
      for _, globbed in ipairs(glob(match)) do
        for i, result in ipairs(results) do
          table.insert(newResults, result .. globbed)
        end
      end
      results = newResults
    end
    init = init + #match
  until init > #value
  return results
end

local function execute(env, command, ...)
  local parts, reason = text.tokenize(command)
  if not parts then
    return false, reason
  elseif #parts == 0 then
    return true
  end
  local program, args = shell.resolveAlias(parts[1], table.pack(select(2, table.unpack(parts))))
  local eargs = {}
  program = evaluate(program)
  for i = 2, #program do
    table.insert(eargs, program[i])
  end
  local program, reason = shell.resolve(program[1], "lua")
  if not program then
    return false, reason
  end
  for i = 1, #args do
    for _, arg in ipairs(evaluate(args[i])) do
      table.insert(eargs, arg)
    end
  end
  args = eargs
  for _, arg in ipairs(table.pack(...)) do
    table.insert(args, arg)
  end
  table.insert(args, 1, true)
  args.n = #args
  local thread, reason = process.load(program, env, nil, command)
  if not thread then
    return false, reason
  end 
  os.setenv("_", program)
  local result = nil
  -- Emulate CC behavior by making yields a filtered event.pull()
  while args[1] and coroutine.status(thread) ~= "dead" do
    result = table.pack(coroutine.resume(thread, table.unpack(args, 2, args.n)))
    if coroutine.status(thread) ~= "dead" then
      args = table.pack(pcall(event.pull, table.unpack(result, 2, result.n)))
    end
  end
  if not args[1] then
    return false, debug.traceback(thread, args[2])
  end
  if not result[1] then
    if type(result[2]) == "table" and result[2].reason == "terminated" then
      if result[2].code then
        return true
      else
        return false, "terminated"
      end
    elseif type(result[2]) == "string" then
      result[2] = debug.traceback(thread, result[2])
    end
  end
  return table.unpack(result, 1, result.n)
end

local args, options = shell.parse(...)
local history = {}

local function getMatchingPrograms(baseName)
  local result = {}
  -- TODO only matching files with .lua extension for now, might want to
  --      extend this to other extensions at some point? env var? file attrs?
  if not baseName or #baseName == 0 then
    baseName = "^(.*)%.lua$"
  else
    baseName = "^(" .. baseName .. ".*)%.lua$"
  end
  for basePath in string.gmatch(os.getenv("PATH"), "[^:]+") do
    for file in fs.list(basePath) do
      local match = file:match(baseName)
      if match then
        table.insert(result, match)
      end
    end
  end
  return result
end

local function getMatchingFiles(baseName)
  local result, basePath = {}
  -- note: we strip the trailing / to make it easier to navigate through
  -- directories using tab completion (since entering the / will then serve
  -- as the intention to go into the currently hinted one).
  -- if we have a directory but no trailing slash there may be alternatives
  -- on the same level, so don't look inside that directory... (cont.)
  if fs.isDirectory(baseName) and baseName:sub(-1) == "/" then
    basePath = baseName
    baseName = "^(.-)/?$"
  else
    basePath = fs.path(baseName) or "/"
    baseName = "^(" .. fs.name(baseName) .. ".-)/?$"
  end
  for file in fs.list(basePath) do
    local match = file:match(baseName)
    if match then
      table.insert(result, fs.concat(basePath, match))
    end
  end
  -- (cont.) but if there's only one match and it's a directory, *then* we
  -- do want to add the trailing slash here.
  if #result == 1 and fs.isDirectory(result[1]) then
    result[1] = result[1] .. "/"
  end
  return result
end

local function hintHandler(line, cursor)
  local line = unicode.sub(line, 1, cursor - 1)
  if not line or #line < 1 then
    return nil
  end
  local result
  local prefix, partial = string.match(line, "^(.+%s)(.+)$")
  local searchInPath = not prefix and not line:find("/")
  if searchInPath then
    -- first part and no path, look for programs in the $PATH
    result = getMatchingPrograms(line)
  else -- just look normal files
    result = getMatchingFiles(shell.resolve(partial or line))
  end
  for i = 1, #result do
    result[i] = (prefix or "") .. result[i] .. (searchInPath and " " or "")
  end
  table.sort(result)
  return result
end

if #args == 0 and (io.input() == io.stdin or options.i) and not options.c then
  -- interactive shell.
  while true do
    if not term.isAvailable() then -- don't clear unless we lost the term
      while not term.isAvailable() do
        event.pull("term_available")
      end
      term.clear()
    end
    while term.isAvailable() do
      local foreground = component.gpu.setForeground(0xFF0000)
      term.write(expand(os.getenv("PS1") or "$ "))
      component.gpu.setForeground(foreground)
      local command = term.read(history, nil, hintHandler)
      if not command then
        term.write("exit\n")
        return -- eof
      end
      while #history > (tonumber(os.getenv("HISTSIZE")) or 10) do
        table.remove(history, 1)
      end
      command = text.trim(command)
      if command == "exit" then
        return
      elseif command ~= "" then
        local result, reason = os.execute(command)
        if term.getCursor() > 1 then
          term.write("\n")
        end
        if not result then
          io.stderr:write((tostring(reason) or "unknown error").. "\n")
        end
      end
    end
  end
else
  -- execute command.
  local result = table.pack(execute(...))
  if not result[1] then
    error(result[2], 0)
  end
  return table.unpack(result, 2)
end
